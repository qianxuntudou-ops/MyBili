package com.tutu.myblbl.core.ui.image

import android.app.ActivityManager
import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.load.engine.executor.GlideExecutor
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.engine.cache.LruResourceCache
import com.bumptech.glide.load.engine.cache.DiskLruCacheFactory
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.Registry
import com.tutu.myblbl.network.NetworkManager
import java.io.File
import java.io.InputStream

@GlideModule
class MyBLBLGlideModule : AppGlideModule() {

    override fun applyOptions(context: Context, builder: GlideBuilder) {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryClass = activityManager.memoryClass // MB
        val cacheSize = (memoryClass * 1024L * 1024 * 0.15).toLong() // 可用内存的15%
        builder.setMemoryCache(LruResourceCache(cacheSize))
        val cacheDir = File(context.externalCacheDir ?: context.cacheDir, "glide_cache")
        builder.setDiskCache(DiskLruCacheFactory(cacheDir.absolutePath, 512L * 1024 * 1024))

        // 首屏一次性加载 12 张封面，5 个线程减少排队等待；TV 设备 CPU 核心数一般 ≥ 4
        builder.setSourceExecutor(
            GlideExecutor.newSourceBuilder().setThreadCount(5).build()
        )
    }

    override fun registerComponents(
        context: Context,
        glide: Glide,
        registry: Registry
    ) {
        registry.replace(
            GlideUrl::class.java,
            InputStream::class.java,
            OkHttpUrlLoader.Factory(NetworkManager.getOkHttpClient())
        )
    }

    override fun isManifestParsingEnabled(): Boolean = false
}
