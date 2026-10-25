package com.cosmos.unreddit.data.model.preferences

import com.cosmos.unreddit.data.model.preferences.UiPreferences.PostLayout
import org.junit.Assert.assertEquals
import org.junit.Test

class PostLayoutTest {

    @Test
    fun fromIndex_followsThePreferenceLabels() {
        assertEquals(PostLayout.CARD, PostLayout.fromIndex(0))
        assertEquals(PostLayout.COMPACT, PostLayout.fromIndex(1))
    }

    @Test
    fun fromIndex_fallsBackToCards() {
        assertEquals(PostLayout.CARD, PostLayout.fromIndex(-1))
        assertEquals(PostLayout.CARD, PostLayout.fromIndex(42))
    }
}
