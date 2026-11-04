package com.example.services

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.os.Environment
import android.provider.MediaStore
import com.example.data.model.AudioEntity
import com.example.data.model.VideoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MediaScannerService(private val context: Context) {

    private val videoExtensions = setOf("mp4", "mkv", "webm", "avi", "mov", "3gp", "ts", "flv", "m4v")
    private val audioExtensions = setOf("mp3", "m4a", "aac", "wav", "flac", "ogg", "opus")

    suspend fun scanVideos(): List<VideoEntity> = withContext(Dispatchers.IO) {
        val videoMap = mutableMapOf<String, VideoEntity>()

        // 1. Query MediaStore
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.RESOLUTION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED
        )
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
                val pathColumn = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val resolutionColumn = cursor.getColumnIndex(MediaStore.Video.Media.RESOLUTION)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    var path = if (pathColumn != -1) cursor.getString(pathColumn) ?: "" else ""
                    if (path.isBlank()) {
                        path = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id).toString()
                    }

                    var title = cursor.getString(titleColumn) ?: ""
                    if (title.isBlank() || title == "Unknown") {
                        title = path.substringAfterLast('/').ifEmpty { "Video $id" }
                    }

                    val duration = cursor.getLong(durationColumn)
                    val resolution = if (resolutionColumn != -1) cursor.getString(resolutionColumn) ?: "1080p" else "1080p"
                    val size = cursor.getLong(sizeColumn)

                    val video = VideoEntity(
                        id = id.toInt(),
                        title = title,
                        path = path,
                        duration = duration,
                        resolution = resolution.ifEmpty { "1080p" },
                        size = size,
                        lastPlayed = System.currentTimeMillis()
                    )
                    videoMap[path] = video
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Direct File System Traversal for unindexed video files
        val scanPathsToRegister = mutableListOf<String>()
        val publicDirs = listOfNotNull(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            Environment.getExternalStorageDirectory()
        )

        val mmr = MediaMetadataRetriever()
        for (dir in publicDirs) {
            if (!dir.exists() || !dir.isDirectory) continue
            scanDirectoryForVideos(dir, videoMap, scanPathsToRegister, mmr)
        }
        try { mmr.release() } catch (_: Exception) {}

        // Trigger system MediaScannerConnection for unindexed files so MediaStore indices update
        if (scanPathsToRegister.isNotEmpty()) {
            try {
                MediaScannerConnection.scanFile(
                    context,
                    scanPathsToRegister.toTypedArray(),
                    null,
                    null
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        videoMap.values.toList()
    }

    private fun scanDirectoryForVideos(
        dir: File,
        videoMap: MutableMap<String, VideoEntity>,
        scanPathsToRegister: MutableList<String>,
        mmr: MediaMetadataRetriever,
        depth: Int = 0
    ) {
        if (depth > 5) return // Prevent deep recursion
        val files = dir.listFiles() ?: return

        for (file in files) {
            if (file.isDirectory) {
                if (!file.name.startsWith(".")) {
                    scanDirectoryForVideos(file, videoMap, scanPathsToRegister, mmr, depth + 1)
                }
            } else if (file.isFile) {
                val ext = file.extension.lowercase()
                if (ext in videoExtensions) {
                    val filePath = file.absolutePath
                    if (!videoMap.containsKey(filePath)) {
                        var duration = 0L
                        var width = 0
                        var height = 0
                        try {
                            mmr.setDataSource(filePath)
                            val durStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                            duration = durStr?.toLongOrNull() ?: 0L
                            val wStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                            val hStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                            width = wStr?.toIntOrNull() ?: 0
                            height = hStr?.toIntOrNull() ?: 0
                        } catch (_: Exception) {}

                        val resolution = if (width > 0 && height > 0) "${width}x${height}" else "1080p"

                        val entity = VideoEntity(
                            id = filePath.hashCode(),
                            title = file.name,
                            path = filePath,
                            duration = duration,
                            resolution = resolution,
                            size = file.length(),
                            lastPlayed = System.currentTimeMillis()
                        )
                        videoMap[filePath] = entity
                        scanPathsToRegister.add(filePath)
                    }
                }
            }
        }
    }

    suspend fun scanAudio(): List<AudioEntity> = withContext(Dispatchers.IO) {
        val audioMap = mutableMapOf<String, AudioEntity>()

        // 1. Query MediaStore
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.SIZE
        )
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val pathColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    var path = if (pathColumn != -1) cursor.getString(pathColumn) ?: "" else ""
                    if (path.isBlank()) {
                        path = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id).toString()
                    }

                    var title = cursor.getString(titleColumn) ?: ""
                    if (title.isBlank() || title == "Unknown") {
                        title = path.substringAfterLast('/').ifEmpty { "Track $id" }
                    }

                    val duration = cursor.getLong(durationColumn)
                    val artist = cursor.getString(artistColumn)?.takeIf { it != "<unknown>" } ?: "Unknown Artist"
                    val album = cursor.getString(albumColumn)?.takeIf { it != "<unknown>" } ?: "Unknown Album"
                    val albumId = cursor.getLong(albumIdColumn)
                    val size = cursor.getLong(sizeColumn)

                    val audio = AudioEntity(
                        id = id.toInt(),
                        title = title,
                        path = path,
                        duration = duration,
                        artist = artist,
                        album = album,
                        albumId = albumId,
                        size = size
                    )
                    audioMap[path] = audio
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Direct File System Traversal for unindexed audio files
        val scanPathsToRegister = mutableListOf<String>()
        val publicDirs = listOfNotNull(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS),
            Environment.getExternalStorageDirectory()
        )

        val mmr = MediaMetadataRetriever()
        for (dir in publicDirs) {
            if (!dir.exists() || !dir.isDirectory) continue
            scanDirectoryForAudio(dir, audioMap, scanPathsToRegister, mmr)
        }
        try { mmr.release() } catch (_: Exception) {}

        if (scanPathsToRegister.isNotEmpty()) {
            try {
                MediaScannerConnection.scanFile(
                    context,
                    scanPathsToRegister.toTypedArray(),
                    null,
                    null
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        audioMap.values.toList()
    }

    private fun scanDirectoryForAudio(
        dir: File,
        audioMap: MutableMap<String, AudioEntity>,
        scanPathsToRegister: MutableList<String>,
        mmr: MediaMetadataRetriever,
        depth: Int = 0
    ) {
        if (depth > 5) return
        val files = dir.listFiles() ?: return

        for (file in files) {
            if (file.isDirectory) {
                if (!file.name.startsWith(".")) {
                    scanDirectoryForAudio(file, audioMap, scanPathsToRegister, mmr, depth + 1)
                }
            } else if (file.isFile) {
                val ext = file.extension.lowercase()
                if (ext in audioExtensions) {
                    val filePath = file.absolutePath
                    if (!audioMap.containsKey(filePath)) {
                        var duration = 0L
                        var artist = "Unknown Artist"
                        var album = "Unknown Album"
                        try {
                            mmr.setDataSource(filePath)
                            val durStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                            duration = durStr?.toLongOrNull() ?: 0L
                            artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown Artist"
                            album = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "Unknown Album"
                        } catch (_: Exception) {}

                        val entity = AudioEntity(
                            id = filePath.hashCode(),
                            title = file.name,
                            path = filePath,
                            duration = duration,
                            artist = artist,
                            album = album,
                            albumId = 0L,
                            size = file.length()
                        )
                        audioMap[filePath] = entity
                        scanPathsToRegister.add(filePath)
                    }
                }
            }
        }
    }
}
