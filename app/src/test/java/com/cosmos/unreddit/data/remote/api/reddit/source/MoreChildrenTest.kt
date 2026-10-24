package com.cosmos.unreddit.data.remote.api.reddit.source

import com.cosmos.unreddit.data.model.Sort
import com.cosmos.unreddit.data.model.TimeSorting
import com.cosmos.unreddit.data.remote.api.reddit.TedditApi
import com.cosmos.unreddit.data.remote.api.reddit.model.Child
import com.cosmos.unreddit.data.remote.api.reddit.model.CommentChild
import com.cosmos.unreddit.data.remote.api.reddit.model.Listing
import com.cosmos.unreddit.data.remote.api.reddit.model.MoreChild
import com.cosmos.unreddit.data.remote.api.reddit.model.TedditUser
import com.cosmos.unreddit.di.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/** TedditSource.getMoreChildren rebuilds /api/morechildren from comment permalinks. */
class MoreChildrenTest {

    // Thread on post "p1":
    //   c1 (depth 0)
    //   └ c2 (depth 1)
    //     ├ c3 (depth 2)
    //     │ └ c4 (depth 3)
    //     └ c5 (depth 2)
    // "More" stubs: under c2 (depth 2) for c3 + c5 + c4 (Reddit lists descendants too),
    // and under the post (depth 0) for c1.
    private val comments = mapOf(
        "c1" to comment("c1", "t3_p1", 0),
        "c2" to comment("c2", "t1_c1", 1),
        "c3" to comment("c3", "t1_c2", 2),
        "c4" to comment("c4", "t1_c3", 3),
        "c5" to comment("c5", "t1_c2", 2),
    )
    private val children = mapOf("c1" to listOf("c2"), "c2" to listOf("c3", "c5"), "c3" to listOf("c4"))

    private val requested = mutableListOf<String>()
    private var failing = emptySet<String>()

    private val source = TedditSource(FakeApi(), NetworkModule.provideRedditMoshi(), Dispatchers.Unconfined)

    @Test
    fun repliesToAComment_comeFromOneRequestWithAbsoluteDepths() = runBlocking {
        val result = source.getMoreChildren(listOf("c3", "c5", "c4"), "t3_p1", "t1_c2", depth = 2, id = "c3")
        val things = result.json.data.things

        assertEquals(listOf("comments/p1/_/c2"), requested)
        assertEquals(listOf("c3" to 2, "c4" to 3, "c5" to 2), things.map { it.idAndDepth() })
        // Flat, as /api/morechildren returns them
        things.forEach { assertEquals(null, (it as CommentChild).data.replies) }
    }

    @Test
    fun topLevelComments_comeOneRequestEach_andDescendantsAreDropped() = runBlocking {
        val result = source.getMoreChildren(listOf("c1", "c2"), "p1", "t3_p1", depth = 0, id = "c1")

        assertEquals(setOf("comments/p1/_/c1", "comments/p1/_/c2"), requested.toSet())
        // c2 came with c1 (its ancestor); fetched on its own, its parent is not the post
        assertEquals(
            listOf("c1" to 0, "c2" to 1, "c3" to 2, "c4" to 3, "c5" to 2),
            result.json.data.things.map { it.idAndDepth() }
        )
    }

    @Test
    fun failedComments_comeBackAsAMoreStub() = runBlocking {
        failing = setOf("c1")
        val result = source.getMoreChildren(listOf("c1", "missing"), "p1", "t3_p1", depth = 0, id = "c1")
        val things = result.json.data.things

        assertEquals(1, things.size)
        val more = (things.single() as MoreChild).data
        assertEquals(listOf("c1"), more.children)
        assertEquals("t3_p1", more.parentId)
        assertEquals(0, more.depth)
    }

    @Test(expected = HttpException::class)
    fun allFailed_throws() {
        failing = setOf("c1")
        runBlocking { source.getMoreChildren(listOf("c1"), "p1", "t3_p1", depth = 0, id = "c1") }
    }

    @Test
    fun cursorUnderAComment_isOneRequest_withDepthsFromTheNesting() = runBlocking {
        // redlib-tor-api: a stub without ids; its id is a cursor. The answer lists c2's
        // remaining replies at the top level, without depths, and ends with a new stub.
        val result = source.getMoreChildren(emptyList(), "t3_p1", "t1_c2", depth = 2, id = "cursor1")
        val things = result.json.data.things

        assertEquals(listOf("comments/p1/_/c2?cursor=cursor1"), requested)
        assertEquals(listOf("c3" to 2, "c4" to 3, "c5" to 2), things.dropLast(1).map { it.idAndDepth() })
        val more = (things.last() as MoreChild).data
        assertEquals("cursor2", more.id)
        assertEquals(2, more.depth)
        assertEquals(emptyList<String>(), more.children)
    }

    @Test
    fun cursorUnderThePost_loadsTheNextTopLevelComments() = runBlocking {
        val result = source.getMoreChildren(emptyList(), "p1", "t3_p1", depth = 0, id = "cursor1")

        assertEquals(listOf("comments/p1?cursor=cursor1"), requested)
        assertEquals(
            listOf("c1" to 0, "c2" to 1, "c3" to 2, "c4" to 3, "c5" to 2),
            result.json.data.things.dropLast(1).map { it.idAndDepth() }
        )
    }

    private fun Child.idAndDepth(): Pair<String, Int?> = when (this) {
        is CommentChild -> data.id to data.depth
        else -> error("unexpected $this")
    }

    /** The thread focused on [id]: the comment (depth 0) and its replies, as Reddit sends it. */
    private fun thread(id: String): String {
        fun tree(id: String, depth: Int): JSONObject {
            val data = JSONObject(comments.getValue(id).toString()).put("depth", depth)
            val replies = children[id].orEmpty()
            data.put(
                "replies",
                if (replies.isEmpty()) "" else listing(replies.map { tree(it, depth + 1) })
            )
            return JSONObject().put("kind", "t1").put("data", data)
        }
        return JSONArray()
            .put(listing(emptyList()))
            .put(listing(listOf(tree(id, 0))))
            .toString()
    }

    /** A cursor page: [ids] as top-level trees without depths, then a stub for the rest. */
    private fun cursorPage(ids: List<String>, parent: String): String {
        fun tree(id: String): JSONObject {
            val data = JSONObject(comments.getValue(id).toString())
            data.remove("depth")
            val replies = children[id].orEmpty()
            data.put("replies", if (replies.isEmpty()) "" else listing(replies.map { tree(it) }))
            return JSONObject().put("kind", "t1").put("data", data)
        }
        val more = JSONObject().put("kind", "more").put(
            "data",
            JSONObject().put("id", "cursor2").put("name", "cursor2").put("count", 3)
                .put("parent_id", parent).put("children", JSONArray())
        )
        return JSONArray()
            .put(listing(emptyList()))
            .put(listing(ids.map { tree(it) } + more))
            .toString()
    }

    private fun listing(children: List<JSONObject>): JSONObject = JSONObject()
        .put("kind", "Listing")
        .put("data", JSONObject().put("children", JSONArray(children)).put("after", JSONObject.NULL))

    private fun comment(id: String, parent: String, depth: Int): JSONObject = JSONObject()
        .put("id", id)
        .put("name", "t1_$id")
        .put("parent_id", parent)
        .put("link_id", "t3_p1")
        .put("depth", depth)
        .put("author", "user")
        .put("body_html", "<p>text</p>")
        .put("score", 1)
        .put("total_awards_received", 0)
        .put("all_awardings", JSONArray())
        .put("edited", false)
        .put("is_submitter", false)
        .put("stickied", false)
        .put("score_hidden", false)
        .put("permalink", "/r/test/comments/p1/_/$id/")
        .put("created_utc", 1_700_000_000)
        .put("controversiality", 0)
        .put("subreddit_name_prefixed", "r/test")

    private inner class FakeApi : TedditApi {
        override suspend fun getCommentThread(postId: String, commentId: String, sort: Sort?, cursor: String?): ResponseBody {
            if (cursor != null) {
                requested.add("comments/$postId/_/$commentId?cursor=$cursor")
                return cursorPage(children[commentId].orEmpty(), "t1_$commentId")
                    .toResponseBody("application/json".toMediaType())
            }
            requested.add("comments/$postId/_/$commentId")
            if (commentId in failing) {
                throw HttpException(Response.error<Any>(429, "".toResponseBody(null)))
            }
            if (commentId !in comments) {
                throw HttpException(Response.error<Any>(404, "".toResponseBody(null)))
            }
            return thread(commentId).toResponseBody("application/json".toMediaType())
        }

        override suspend fun getSubreddit(subreddit: String, sort: Sort, timeSorting: TimeSorting?, after: String?): Listing = error("unused")
        override suspend fun getSubredditInfo(subreddit: String): Child = error("unused")
        override suspend fun searchInSubreddit(subreddit: String, query: String, sort: Sort?, timeSorting: TimeSorting?, after: String?): Listing = error("unused")
        override suspend fun getPost(permalink: String, limit: Int?, sort: Sort): List<Listing> = error("unused")
        override suspend fun getUserPosts(user: String, sort: Sort, timeSorting: TimeSorting?, after: String?): TedditUser = error("unused")
        override suspend fun getUserComments(user: String, sort: Sort, timeSorting: TimeSorting?, after: String?): TedditUser = error("unused")
        override suspend fun searchPost(query: String, sort: Sort?, timeSorting: TimeSorting?, after: String?, limit: Int?): Listing = error("unused")
        override suspend fun getCommentPage(postId: String, cursor: String, sort: Sort?): ResponseBody {
            requested.add("comments/$postId?cursor=$cursor")
            return cursorPage(listOf("c1"), "t3_$postId").toResponseBody("application/json".toMediaType())
        }

        override suspend fun searchUser(query: String, after: String?): Listing = error("unused")
        override suspend fun searchSubreddit(query: String, sort: Sort?, timeSorting: TimeSorting?, after: String?): Listing = error("unused")
    }
}
