package com.cosmos.unreddit.ui.instance

import com.cosmos.unreddit.data.remote.instance.Instance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/** Checks that an instance answers, and accepts the credentials, before it is saved. */
class InstanceTester(okHttpClient: OkHttpClient) {

    sealed class Result {
        object Ok : Result()
        object Unauthorized : Result()
        class HttpError(val code: Int) : Result()
        class NetworkError(val message: String) : Result()
    }

    // Without the app's auth interceptor: the instance being tested brings its own credentials
    private val client = okHttpClient.newBuilder()
        .apply { networkInterceptors().clear() }
        .build()

    suspend fun test(instance: Instance): Result = withContext(Dispatchers.IO) {
        try {
            // redlib-tor-api's health check; other Teddit API instances get a one-post listing
            val health = request(instance, "healthz")
            when {
                health.isSuccessful -> Result.Ok
                health.code == 401 || health.code == 403 -> Result.Unauthorized
                else -> {
                    val listing = request(instance, "hot?api&target=reddit&limit=1")
                    when {
                        listing.isSuccessful -> Result.Ok
                        listing.code == 401 || listing.code == 403 -> Result.Unauthorized
                        else -> Result.HttpError(listing.code)
                    }
                }
            }
        } catch (e: IOException) {
            Result.NetworkError(e.message ?: e.javaClass.simpleName)
        }
    }

    private data class Response(val code: Int, val isSuccessful: Boolean)

    private fun request(instance: Instance, path: String): Response {
        val url = instance.url.resolve(path) ?: throw IOException("Invalid URL")
        val request = Request.Builder()
            .url(url)
            .apply { instance.authorization?.let { header("Authorization", it) } }
            .build()
        return client.newCall(request).execute().use { Response(it.code, it.isSuccessful) }
    }
}
