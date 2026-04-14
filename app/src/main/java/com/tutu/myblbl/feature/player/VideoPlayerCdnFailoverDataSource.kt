package com.tutu.myblbl.feature.player

import android.net.Uri
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import java.io.IOException

internal class VideoPlayerCdnFailoverState(
    val candidates: List<Uri>
) {
    @Volatile
    private var preferredIndex: Int = 0

    @Synchronized
    fun preferredIndex(): Int {
        val lastIndex = candidates.lastIndex.coerceAtLeast(0)
        return preferredIndex.coerceIn(0, lastIndex)
    }

    @Synchronized
    fun markPreferred(index: Int) {
        val lastIndex = candidates.lastIndex.coerceAtLeast(0)
        preferredIndex = index.coerceIn(0, lastIndex)
    }
}

internal class VideoPlayerCdnFailoverDataSourceFactory(
    private val upstreamFactory: DataSource.Factory,
    private val state: VideoPlayerCdnFailoverState
) : DataSource.Factory {
    override fun createDataSource(): DataSource {
        return VideoPlayerCdnFailoverDataSource(
            upstreamFactory = upstreamFactory,
            state = state
        )
    }
}

internal class VideoPlayerCdnFailoverDataSource(
    private val upstreamFactory: DataSource.Factory,
    private val state: VideoPlayerCdnFailoverState
) : DataSource {

    private var upstream: DataSource? = null
    private val transferListeners = ArrayList<TransferListener>(2)

    override fun addTransferListener(transferListener: TransferListener) {
        transferListeners += transferListener
        upstream?.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        closeQuietly()
        val candidates = state.candidates
        if (candidates.isEmpty()) {
            throw IOException("No CDN candidates available")
        }

        val startIndex = state.preferredIndex()
        var lastException: IOException? = null
        for (attempt in candidates.indices) {
            val index = (startIndex + attempt) % candidates.size
            val candidateUri = candidates[index]
            val upstreamSource = upstreamFactory.createDataSource()
            transferListeners.forEach(upstreamSource::addTransferListener)
            val candidateSpec = dataSpec.buildUpon()
                .setUri(candidateUri)
                .build()
            try {
                val openedLength = upstreamSource.open(candidateSpec)
                upstream = upstreamSource
                state.markPreferred(index)
                return openedLength
            } catch (error: IOException) {
                runCatching { upstreamSource.close() }
                lastException = error
            }
        }

        throw lastException ?: IOException("Failed to open any CDN candidate")
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        val activeUpstream = upstream ?: throw IllegalStateException("read() before open()")
        return activeUpstream.read(buffer, offset, length)
    }

    override fun getUri(): Uri? = upstream?.uri

    override fun close() {
        closeQuietly()
    }

    private fun closeQuietly() {
        runCatching { upstream?.close() }
        upstream = null
    }
}
