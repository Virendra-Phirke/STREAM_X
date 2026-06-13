package com.example.ui.components

import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import android.media.MediaMetadataRetriever
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import okio.Buffer

class AudioCoverFetcher(
    private val path: String,
    private val options: Options
) : Fetcher {
    companion object {
        private val semaphore = Semaphore(4)
    }

    override suspend fun fetch(): FetchResult? {
        return withContext(Dispatchers.IO) {
            semaphore.withPermit {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(path)
                    val picture = retriever.embeddedPicture
                    if (picture != null) {
                        SourceResult(
                            source = ImageSource(
                                source = Buffer().write(picture),
                                context = options.context
                            ),
                            mimeType = null,
                            dataSource = DataSource.MEMORY
                        )
                    } else {
                        null
                    }
                } catch (e: Throwable) {
                    null
                } finally {
                    try { retriever.release() } catch (e: Throwable) {}
                }
            }
        }
    }

    class Factory : Fetcher.Factory<String> {
        override fun create(data: String, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (data.endsWith(".mp3", true) || data.endsWith(".m4a", true) || data.endsWith(".flac", true) || data.endsWith(".ogg", true) || data.endsWith(".wav", true)) {
                return AudioCoverFetcher(data, options)
            }
            return null
        }
    }
}

