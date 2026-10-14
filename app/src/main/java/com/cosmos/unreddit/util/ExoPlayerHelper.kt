package com.cosmos.unreddit.util

import android.content.Context
import com.cosmos.unreddit.di.NetworkModule.InstanceOkHttp
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSource
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient

class ExoPlayerHelper(context: Context) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ExoPlayerHelperEntryPoint {
        @InstanceOkHttp
        fun okHttpClient(): OkHttpClient
    }

    private val cacheDir = context.cacheDir

    // Shares the app's client, so instance media gets the instance's credentials
    private val httpDataSourceFactory = OkHttpDataSource.Factory(
        EntryPointAccessors
            .fromApplication(context.applicationContext, ExoPlayerHelperEntryPoint::class.java)
            .okHttpClient()
    )
        .setUserAgent(LinkUtil.USER_AGENT)

    private val exoDatabaseProvider = ExoDatabaseProvider(context)

    private val simpleCache = SimpleCache(
        cacheDir,
        LeastRecentlyUsedCacheEvictor(100 * 1024 * 1024),
        exoDatabaseProvider
    )

    private val cacheDataSourceFactory = CacheDataSource.Factory()
        .setCache(simpleCache)
        .setUpstreamDataSourceFactory(httpDataSourceFactory)

    val defaultMediaSourceFactory by lazy { DefaultMediaSourceFactory(cacheDataSourceFactory) }

    fun setRequestProperties(requestProperties: Map<String, String>) {
        httpDataSourceFactory.setDefaultRequestProperties(requestProperties)
    }

    fun getMediaItem(url: String): MediaItem {
        return MediaItem.fromUri(url)
    }

    fun getMediaSource(url: String): MediaSource {
        return getMediaSource(getMediaItem(url))
    }

    fun getMediaSource(mediaItem: MediaItem): MediaSource {
        return defaultMediaSourceFactory.createMediaSource(mediaItem)
    }

    fun clearCache() {
        simpleCache.release()
        SimpleCache.delete(cacheDir, exoDatabaseProvider)
    }
}
