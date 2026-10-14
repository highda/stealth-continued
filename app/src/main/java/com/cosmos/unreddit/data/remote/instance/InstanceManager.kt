package com.cosmos.unreddit.data.remote.instance

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Credentials
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject
import javax.inject.Singleton

/** A Teddit API instance and its optional HTTP Basic credentials. */
data class Instance(
    val url: HttpUrl,
    val username: String = "",
    val password: String = ""
) {
    val hasCredentials: Boolean
        get() = username.isNotEmpty()

    /** `Authorization` header value, or null without credentials. */
    val authorization: String?
        get() = if (hasCredentials) Credentials.basic(username, password, Charsets.UTF_8) else null

    /** Whether [other] is served by this instance (same scheme, host and port). */
    fun serves(other: HttpUrl): Boolean {
        return other.scheme == url.scheme && other.host == url.host && other.port == url.port
    }

    companion object {
        /**
         * Parses what a user typed: `host`, `host:port` or a full `http(s)://` URL. Defaults to
         * HTTPS. Credentials typed into the URL are rejected: they belong in their own fields.
         */
        fun parseUrl(input: String): HttpUrl? {
            val trimmed = input.trim().trimEnd('/')
            if (trimmed.isEmpty()) return null
            val withScheme = if (trimmed.contains("://")) trimmed else "https://$trimmed"
            val url = withScheme.toHttpUrlOrNull() ?: return null
            if (url.scheme != "http" && url.scheme != "https") return null
            if (url.username.isNotEmpty() || url.password.isNotEmpty()) return null
            return url.newBuilder().encodedPath("/").query(null).fragment(null).build()
        }
    }
}

/**
 * The configured instance. The URL and username are stored in preferences; the password is
 * stored encrypted with a Keystore key (see [CredentialCipher]).
 */
@Singleton
class InstanceManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    private val _instance = MutableStateFlow(runBlocking { load() })
    val instance: StateFlow<Instance?> = _instance.asStateFlow()

    /** The instance used for requests right now. */
    val current: Instance?
        get() = _instance.value

    val isConfigured: Boolean
        get() = current != null

    /**
     * Saves the instance. [password] null keeps the stored password (unless the username is
     * cleared, which removes it); an empty string removes it.
     */
    suspend fun save(url: HttpUrl, username: String, password: String?) {
        val user = username.trim()
        val newPassword = when {
            user.isEmpty() -> ""
            password == null -> current?.password.orEmpty()
            else -> password
        }
        dataStore.edit { prefs ->
            prefs[KEY_URL] = url.toString()
            prefs[KEY_USERNAME] = user
            if (newPassword.isEmpty()) {
                prefs.remove(KEY_PASSWORD)
            } else {
                prefs[KEY_PASSWORD] = CredentialCipher.encrypt(newPassword)
            }
        }
        _instance.value = Instance(url, user, newPassword)
    }

    private suspend fun load(): Instance? {
        val prefs = dataStore.data.first()
        val url = prefs[KEY_URL]?.let { Instance.parseUrl(it) } ?: return null
        val password = prefs[KEY_PASSWORD]?.let { CredentialCipher.decrypt(it) }.orEmpty()
        return Instance(url, prefs[KEY_USERNAME].orEmpty(), password)
    }

    companion object {
        private val KEY_URL = stringPreferencesKey("instance_url")
        private val KEY_USERNAME = stringPreferencesKey("instance_username")
        private val KEY_PASSWORD = stringPreferencesKey("instance_password_encrypted")
    }
}
