package com.cosmos.unreddit.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LinkUtilSoundTrackTest {

    @Test
    fun dashVideosHaveASeparateSoundTrack() {
        assertEquals(
            "https://example.org/media/v.redd.it/abc/DASH_AUDIO_128.mp4",
            LinkUtil.getRedditSoundTrack("https://example.org/media/v.redd.it/abc/DASH_720.mp4")
        )
    }

    @Test
    fun muxedVideosHaveTheirSoundWithThem() {
        assertNull(
            LinkUtil.getRedditSoundTrack(
                "https://example.org/media/packaged-media.redd.it/abc/muxed-medium.mp4" +
                        "?m=DASHPlaylist.mpd&e=1&s=abc"
            )
        )
        assertNull(LinkUtil.getRedditSoundTrack("https://example.org/media/packaged-media.redd.it/abc/dl/m2-res_1280p.mp4"))
    }
}
