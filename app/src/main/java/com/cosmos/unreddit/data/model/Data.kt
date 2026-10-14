package com.cosmos.unreddit.data.model

import com.cosmos.unreddit.data.model.preferences.ContentPreferences
import com.cosmos.unreddit.data.remote.instance.Instance

sealed class Data {

    data class Fetch(val query: String, val sorting: Sorting) : Data()

    data class FetchMultiple(
        val query: List<String>,
        val sorting: Sorting,
        // The instance the posts come from: a new instance means new data
        val instance: Instance? = null
    ) : Data()

    data class User(
        val history: List<String>,
        val saved: List<String>,
        val contentPreferences: ContentPreferences,
        val savedComments: List<String>? = null
    ) : Data()
}
