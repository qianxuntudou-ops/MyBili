package com.tutu.myblbl.core.ui.image

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.cache.LruResourceCache
import com.bumptech.glide.load.engine.cache.DiskLruCacheFactory
import com.bumptech.glide.module.AppGlideModule
import java.io.File

@GlideModule
class MyBLBLGlideModule : AppGlideModule() {

    override fun applyOptions(context: Context, builder: GlideBuilder) {
        builder.setMemoryCache(LruResourceCache(256L * 1024 * 1024))
        val cacheDir = File(context.externalCacheDir ?: context.cacheDir, "glide_cache")
        builder.setDiskCache(DiskLruCacheFactory(cacheDir.absolutePath, 512L * 1024 * 1024))
    }

    override fun isManifestParsingEnabled(): Boolean = false
}
