package com.cosmos.unreddit.ui.postlist

/** What the home screen shows, picked in the profile drawer. */
sealed interface FeedSelection {

    /** The profile's subscriptions, or r/popular without any. */
    data object Home : FeedSelection

    /** One of Reddit's own feeds, whatever the subscriptions. */
    enum class Reddit(val subreddit: String) : FeedSelection {
        POPULAR("popular"),
        ALL("all")
    }

    data class Custom(val id: Int) : FeedSelection
}
