package com.cosmos.unreddit.data.remote.instance

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InstanceTest {

    @Test
    fun parseUrl_defaultsToHttpsAtTheRoot() {
        assertEquals("https://api.example.org/", Instance.parseUrl("api.example.org").toString())
        assertEquals("https://api.example.org/", Instance.parseUrl(" https://api.example.org/r/rust?api ").toString())
        assertEquals("https://api.example.org:8443/", Instance.parseUrl("api.example.org:8443").toString())
        assertEquals("http://10.0.2.2:8899/", Instance.parseUrl("http://10.0.2.2:8899").toString())
    }

    @Test
    fun parseUrl_rejectsCredentialsAndOtherSchemes() {
        assertNull(Instance.parseUrl(""))
        assertNull(Instance.parseUrl("https://user:pass@api.example.org"))
        assertNull(Instance.parseUrl("ftp://api.example.org"))
        assertNull(Instance.parseUrl("not a url"))
    }

    @Test
    fun serves_onlyTheSameSchemeHostAndPort() {
        val instance = Instance("https://api.example.org/".toHttpUrl(), "user", "pass")
        assertTrue(instance.serves("https://api.example.org/media/i.redd.it/a.jpg".toHttpUrl()))
        assertFalse(instance.serves("http://api.example.org/media/i.redd.it/a.jpg".toHttpUrl()))
        assertFalse(instance.serves("https://api.example.org:8443/".toHttpUrl()))
        assertFalse(instance.serves("https://i.imgur.com/a.jpg".toHttpUrl()))
    }

    @Test
    fun authorization_onlyWithAUsername() {
        assertNull(Instance("https://api.example.org/".toHttpUrl()).authorization)
        assertEquals(
            "Basic dXNlcjpwYXNz",
            Instance("https://api.example.org/".toHttpUrl(), "user", "pass").authorization
        )
    }
}
