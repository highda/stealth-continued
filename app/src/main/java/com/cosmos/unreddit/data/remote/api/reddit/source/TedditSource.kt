package com.cosmos.unreddit.data.remote.api.reddit.source

import com.cosmos.unreddit.data.model.Sort
import com.cosmos.unreddit.data.model.TimeSorting
import com.cosmos.unreddit.data.remote.api.reddit.TedditApi
import com.cosmos.unreddit.data.remote.api.reddit.model.Child
import com.cosmos.unreddit.data.remote.api.reddit.model.Data
import com.cosmos.unreddit.data.remote.api.reddit.model.JsonMore
import com.cosmos.unreddit.data.remote.api.reddit.model.Listing
import com.cosmos.unreddit.data.remote.api.reddit.model.ListingData
import com.cosmos.unreddit.data.remote.api.reddit.model.MoreChildren
import com.cosmos.unreddit.di.DispatchersModule.IoDispatcher
import com.cosmos.unreddit.di.NetworkModule.RedditMoshi
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reddit content through a Teddit API instance (`?api&target=reddit`). Every request goes to
 * the configured instance; nothing is sent to Reddit.
 */
@Singleton
class TedditSource @Inject constructor(
    private val tedditApi: TedditApi,
    @RedditMoshi moshi: Moshi,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    private val jsonAdapter = moshi.adapter(Any::class.java)
    private val childListAdapter = moshi.adapter<List<Child>>(
        Types.newParameterizedType(List::class.java, Child::class.java)
    )

    //region Subreddit

    suspend fun getSubreddit(
        subreddit: String,
        sort: Sort,
        timeSorting: TimeSorting?,
        after: String? = null
    ): Listing {
        if (subreddit.contains('+')) return getMultiSubreddit(subreddit.split('+'), sort, timeSorting, after)
        return tedditApi.getSubreddit(subreddit, sort, timeSorting, after)
    }

    /**
     * Posts from several subreddits (subscriptions, custom feeds).
     *
     * redlib-tor-api forwards `/r/a+b` to Reddit with a literal `+`, which Reddit reads as a
     * space and answers with r/popular. A search restricted to the subreddits returns the same
     * posts in one request, minus stickied posts; search has no rising or controversial sort,
     * so those fall back to hot and top.
     */
    private suspend fun getMultiSubreddit(
        subreddits: List<String>,
        sort: Sort,
        timeSorting: TimeSorting?,
        after: String?
    ): Listing {
        val query = subreddits.joinToString(" OR ") { "subreddit:$it" }
        val searchSort = when (sort) {
            Sort.NEW, Sort.TOP, Sort.COMMENTS -> sort
            Sort.CONTROVERSIAL -> Sort.TOP
            else -> Sort.HOT
        }
        val time = if (searchSort == Sort.TOP) timeSorting ?: TimeSorting.DAY else TimeSorting.ALL
        val listing = tedditApi.searchPost(query, searchSort, time, after)
        if (listing.data.children.isNotEmpty() || listing.data.after != null) return listing
        // Reddit's search now and then answers with nothing, and Redlib caches that for a while;
        // ask once more with a different URL (an explicit default limit) to get past the cache
        return tedditApi.searchPost(query, searchSort, time, after, limit = SEARCH_LIMIT)
    }

    suspend fun getSubredditInfo(subreddit: String): Child {
        return tedditApi.getSubredditInfo(subreddit)
    }

    suspend fun searchInSubreddit(
        subreddit: String,
        query: String,
        sort: Sort?,
        timeSorting: TimeSorting?,
        after: String? = null
    ): Listing {
        return tedditApi.searchInSubreddit(subreddit, query, sort, timeSorting, after)
    }

    //endregion

    suspend fun getPost(permalink: String, limit: Int? = null, sort: Sort): List<Listing> {
        return tedditApi.getPost(permalink, limit, sort)
    }

    /**
     * Loads the comments hidden behind a "more" stub. The Teddit API has no
     * `/api/morechildren`, so this rebuilds its answer from comment permalinks:
     *
     * - A stub under a comment: one request for the thread focused on that comment, keeping
     *   the requested replies with their loaded descendants.
     * - A stub under the post, or IDs the first request did not return: one request per
     *   comment, focused on the comment itself, at most [MORE_PARALLELISM] at a time.
     *
     * Returns Reddit's `/api/morechildren` shape: a flat, depth-first list with absolute depths.
     */
    suspend fun getMoreChildren(
        children: List<String>,
        linkId: String,
        parentId: String,
        depth: Int,
        sort: Sort? = null
    ): MoreChildren = withContext(ioDispatcher) {
        val postId = linkId.removePrefix("t3_")
        val things = mutableListOf<Any?>()
        val found = mutableSetOf<String>()

        if (parentId.startsWith("t1_")) {
            val thread = getThread(postId, parentId.removePrefix("t1_"), sort)
            val parent = thread.firstOrNull()
            // The focused comment has depth 0 and its replies 1; the stub sits at `depth`.
            parent?.replies()?.filter { it.id() in children }?.forEach { reply ->
                flatten(reply, depth - 1, things)
                found.add(reply.id().orEmpty())
            }
        }

        val missing = children.filterNot { it in found }
        if (missing.isNotEmpty()) {
            val semaphore = Semaphore(MORE_PARALLELISM)
            val results = coroutineScope {
                missing.map { id ->
                    async {
                        semaphore.withPermit {
                            try {
                                Result.success(getThread(postId, id, sort).firstOrNull())
                            } catch (e: HttpException) {
                                // Deleted or removed comments answer 404; skip them
                                if (e.code() == 404) Result.success(null) else Result.failure(e)
                            } catch (e: IOException) {
                                Result.failure(e)
                            }
                        }
                    }
                }.awaitAll()
            }
            if (results.all { it.isFailure }) throw results.first().exceptionOrNull()!!

            results.mapNotNull { it.getOrNull() }
                // Reddit lists descendants in a stub too; they come with their ancestors
                .filter { it.data()?.get("parent_id") == parentId }
                .forEach { flatten(it, depth, things) }

            // Comments that failed (rate limit, network) come back as a new "more" stub
            val failed = missing.filterIndexed { index, _ -> results[index].isFailure }
            if (failed.isNotEmpty()) {
                things.add(
                    mapOf(
                        "kind" to "more",
                        "data" to mapOf(
                            "count" to failed.size,
                            "name" to "t1_${failed.first()}_retry",
                            "id" to "${failed.first()}_retry",
                            "parent_id" to parentId,
                            "depth" to depth,
                            "children" to failed
                        )
                    )
                )
            }
        }

        val parsed = childListAdapter.fromJsonValue(things) ?: throw IOException()
        MoreChildren(JsonMore(Data(parsed)))
    }

    //region User

    suspend fun getUserInfo(user: String): Child {
        return tedditApi.getUserPosts(user, Sort.HOT, null).about
    }

    suspend fun getUserPosts(
        user: String,
        sort: Sort,
        timeSorting: TimeSorting?,
        after: String? = null
    ): Listing {
        return tedditApi.getUserPosts(user, sort, timeSorting, after).overview
    }

    suspend fun getUserComments(
        user: String,
        sort: Sort,
        timeSorting: TimeSorting?,
        after: String? = null
    ): Listing {
        return tedditApi.getUserComments(user, sort, timeSorting, after).overview
    }

    //endregion

    //region Search

    suspend fun searchPost(
        query: String,
        sort: Sort?,
        timeSorting: TimeSorting?,
        after: String? = null
    ): Listing {
        return tedditApi.searchPost(query, sort, timeSorting, after)
    }

    /**
     * The Teddit API has no user search, so this looks the query up as an exact username and
     * returns that user, or nothing.
     */
    suspend fun searchUser(
        query: String,
        sort: Sort?,
        timeSorting: TimeSorting?,
        after: String? = null
    ): Listing {
        val name = query.trim().removePrefix("/").removePrefix("u/").removePrefix("user/")
        val user = if (after == null && USERNAME.matches(name)) {
            try {
                getUserInfo(name)
            } catch (e: HttpException) {
                // Unknown, suspended or banned users
                if (e.code() in 400..499) null else throw e
            }
        } else {
            null
        }
        return Listing("Listing", ListingData(null, null, listOfNotNull(user), null, null))
    }

    suspend fun searchSubreddit(
        query: String,
        sort: Sort?,
        timeSorting: TimeSorting?,
        after: String? = null
    ): Listing {
        return tedditApi.searchSubreddit(query, sort, timeSorting, after)
    }

    //endregion

    //region Thread JSON helpers

    /** Top-level children (as JSON maps) of the comment listing of a focused thread. */
    @Suppress("UNCHECKED_CAST")
    private suspend fun getThread(postId: String, commentId: String, sort: Sort?): List<Map<String, Any?>> {
        val body = tedditApi.getCommentThread(postId, commentId, sort)
        val json = body.use { jsonAdapter.fromJson(it.source()) } as? List<Map<String, Any?>>
            ?: throw IOException("Unexpected thread JSON")
        val comments = json.getOrNull(1)?.get("data") as? Map<String, Any?>
        return comments?.get("children") as? List<Map<String, Any?>> ?: emptyList()
    }

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any?>.data(): Map<String, Any?>? = get("data") as? Map<String, Any?>

    private fun Map<String, Any?>.id(): String? = data()?.get("id") as? String

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any?>.replies(): List<Map<String, Any?>> {
        val replies = data()?.get("replies") as? Map<String, Any?> ?: return emptyList()
        return (replies["data"] as? Map<String, Any?>)?.get("children") as? List<Map<String, Any?>>
            ?: emptyList()
    }

    /**
     * Appends [child] and its descendants to [out] depth-first, with `depth` shifted by
     * [shift] and `replies` emptied, as `/api/morechildren` returns them.
     */
    private fun flatten(child: Map<String, Any?>, shift: Int, out: MutableList<Any?>) {
        val data = child.data() ?: return
        val replies = child.replies()
        val newData = data.toMutableMap()
        (data["depth"] as? Number)?.let { newData["depth"] = it.toInt() + shift }
        if (child["kind"] == "t1") newData["replies"] = ""
        out.add(child.toMutableMap().apply { put("data", newData) })
        replies.forEach { flatten(it, shift, out) }
    }

    //endregion

    companion object {
        private const val MORE_PARALLELISM = 4

        private const val SEARCH_LIMIT = 25

        private val USERNAME = Regex("^[A-Za-z0-9_-]{3,20}$")
    }
}
