package com.cosmos.unreddit.data.remote.instance

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/** Thrown for API requests made before an instance is configured. */
class NoInstanceException : IOException("No Teddit API instance configured")

/**
 * Points requests made against [PLACEHOLDER] at the configured instance, so a changed instance
 * takes effect without restarting the app.
 */
class InstanceInterceptor(private val instance: () -> Instance?) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.url.host != PLACEHOLDER.host) return chain.proceed(request)

        val target = instance()?.url ?: throw NoInstanceException()
        val url = request.url.newBuilder()
            .scheme(target.scheme)
            .host(target.host)
            .port(target.port)
            .build()
        return chain.proceed(request.newBuilder().url(url).build())
    }

    companion object {
        val PLACEHOLDER: HttpUrl = "http://instance.invalid/".toHttpUrl()
    }
}

/**
 * Adds the instance's HTTP Basic credentials to requests for the instance, and only those.
 * Installed as a network interceptor, so it is evaluated again for every redirect hop and the
 * credentials never follow a redirect to another host.
 */
class InstanceAuthInterceptor(private val instance: () -> Instance?) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val current = instance()
        val authorization = current?.authorization
        if (authorization == null ||
            !current.serves(request.url) ||
            request.header(HEADER) != null
        ) {
            return chain.proceed(request)
        }
        return chain.proceed(request.newBuilder().header(HEADER, authorization).build())
    }

    companion object {
        private const val HEADER = "Authorization"
    }
}
