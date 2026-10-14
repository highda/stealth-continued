package com.cosmos.unreddit.ui.feed

import org.junit.Assert.assertEquals
import org.junit.Test

class FeedNameTest {

    @Test
    fun normalize_acceptsTheUsualWaysOfWritingASubreddit() {
        assertEquals("rust", FeedEditDialogFragment.normalize("rust"))
        assertEquals("rust", FeedEditDialogFragment.normalize(" r/rust "))
        assertEquals("rust", FeedEditDialogFragment.normalize("/r/rust/"))
        assertEquals("rust", FeedEditDialogFragment.normalize("https://www.reddit.com/r/rust/hot"))
    }
}
