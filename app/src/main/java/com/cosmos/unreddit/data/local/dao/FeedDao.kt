package com.cosmos.unreddit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.cosmos.unreddit.data.model.db.Feed
import com.cosmos.unreddit.data.model.db.FeedSubreddit
import com.cosmos.unreddit.data.model.db.FeedWithSubreddits
import kotlinx.coroutines.flow.Flow

@Dao
abstract class FeedDao {

    @Insert
    abstract suspend fun insert(feed: Feed): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertSubreddits(subreddits: List<FeedSubreddit>)

    @Query("UPDATE feed SET name = :name WHERE id = :feedId")
    abstract suspend fun rename(feedId: Int, name: String)

    @Query("DELETE FROM feed WHERE id = :feedId")
    abstract suspend fun delete(feedId: Int)

    @Query("DELETE FROM feed_subreddit WHERE feed_id = :feedId AND name = :name")
    abstract suspend fun deleteSubreddit(feedId: Int, name: String)

    @Query("DELETE FROM feed_subreddit WHERE feed_id = :feedId")
    abstract suspend fun deleteSubreddits(feedId: Int)

    @Transaction
    @Query("SELECT * FROM feed WHERE profile_id = :profileId ORDER BY time")
    abstract fun getFeedsFromProfile(profileId: Int): Flow<List<FeedWithSubreddits>>

    @Transaction
    @Query("SELECT * FROM feed WHERE id = :feedId")
    abstract fun getFeed(feedId: Int): Flow<FeedWithSubreddits?>

    @Transaction
    @Query("SELECT * FROM feed WHERE profile_id = :profileId ORDER BY time")
    abstract suspend fun getFeedsFromProfileOnce(profileId: Int): List<FeedWithSubreddits>

    /** Creates or updates a feed with exactly [subreddits]. Returns the feed's id. */
    @Transaction
    open suspend fun save(feedId: Int?, name: String, subreddits: List<String>, profileId: Int): Int {
        val now = System.currentTimeMillis()
        val id = if (feedId == null) {
            insert(Feed(name = name, time = now, profileId = profileId)).toInt()
        } else {
            rename(feedId, name)
            deleteSubreddits(feedId)
            feedId
        }
        insertSubreddits(subreddits.mapIndexed { index, sub -> FeedSubreddit(id, sub, now + index) })
        return id
    }
}
