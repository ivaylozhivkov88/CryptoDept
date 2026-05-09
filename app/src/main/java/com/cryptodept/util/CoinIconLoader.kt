package com.cryptodept.util

import android.content.Context
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.ImageRequest
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class CoinIconLoader
    @Inject
    constructor(
        @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
        @Named("PublicClient") private val okHttpClient: OkHttpClient,
    ) {
        val imageLoader =
            ImageLoader
                .Builder(context)
                .okHttpClient(okHttpClient)
                .components {
                    add(SvgDecoder.Factory())
                }.memoryCache {
                    MemoryCache
                        .Builder(context)
                        .maxSizePercent(0.25)
                        .build()
                }.diskCache {
                    DiskCache
                        .Builder()
                        .directory(context.cacheDir.resolve("coin_icons"))
                        .maxSizeBytes(50 * 1024 * 1024)
                        .build()
                }.crossfade(200)
                .build()

        /**
         * Preload icons for the top coins to improve dashboard feel.
         */
        fun preloadIcons(urls: List<String>) {
            urls.forEach { url ->
                val request =
                    ImageRequest
                        .Builder(context)
                        .data(url)
                        .build()
                imageLoader.enqueue(request)
            }
        }
    }
