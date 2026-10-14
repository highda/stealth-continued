package com.cosmos.unreddit.data.model.db

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Relation

/** A custom feed: a named set of subreddits shown together, like Reddit's custom feeds. */
@Entity(
    tableName = "feed",
    foreignKeys = [
        ForeignKey(
            entity = Profile::class,
            parentColumns = ["id"],
            childColumns = ["profile_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Feed(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "time")
    val time: Long,

    @ColumnInfo(name = "profile_id", index = true)
    val profileId: Int
)

@Entity(
    tableName = "feed_subreddit",
    primaryKeys = ["feed_id", "name"],
    foreignKeys = [
        ForeignKey(
            entity = Feed::class,
            parentColumns = ["id"],
            childColumns = ["feed_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class FeedSubreddit(
    @ColumnInfo(name = "feed_id", index = true)
    val feedId: Int,

    @ColumnInfo(name = "name", collate = ColumnInfo.NOCASE)
    val name: String,

    @ColumnInfo(name = "time")
    val time: Long
)

data class FeedWithSubreddits(
    @Embedded
    val feed: Feed,

    @Relation(parentColumn = "id", entityColumn = "feed_id")
    val subreddits: List<FeedSubreddit>
) {
    /** Subreddit names in the order they were added. */
    val names: List<String>
        get() = subreddits.sortedBy { it.time }.map { it.name }
}
