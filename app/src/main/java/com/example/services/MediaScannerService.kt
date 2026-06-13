package com.example.services

import android.content.Context
import android.provider.MediaStore
import com.example.data.model.AudioEntity
import com.example.data.model.VideoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaScannerService(private val context: Context) {

    suspend fun scanVideos(): List<VideoEntity> = withContext(Dispatchers.IO) {
        val videos = mutableListOf<VideoEntity>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.RESOLUTION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED
        )
        // using sort by date added descending
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
                val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val resolutionColumn = cursor.getColumnIndex(MediaStore.Video.Media.RESOLUTION)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)

                while (cursor.moveToNext()) {
                    val title = cursor.getString(titleColumn) ?: "Unknown"
                    val path = cursor.getString(pathColumn) ?: ""
                    val duration = cursor.getLong(durationColumn)
                    val resolution = if (resolutionColumn != -1) cursor.getString(resolutionColumn) else "Unknown"
                    val size = cursor.getLong(sizeColumn)

                    videos.add(
                        VideoEntity(
                            title = title,
                            path = path,
                            duration = duration,
                            resolution = resolution ?: "Unknown",
                            size = size,
                            lastPlayed = 0
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        videos
    }

    suspend fun scanAudio(): List<AudioEntity> = withContext(Dispatchers.IO) {
        val audioList = mutableListOf<AudioEntity>()
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
        // using sort by title
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"
        
        // Ensure only music is selected
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)

                while (cursor.moveToNext()) {
                    val title = cursor.getString(titleColumn) ?: "Unknown"
                    val path = cursor.getString(pathColumn) ?: ""
                    val duration = cursor.getLong(durationColumn)
                    val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                    val album = cursor.getString(albumColumn) ?: "Unknown Album"
                    val albumId = cursor.getLong(albumIdColumn)
                    val size = cursor.getLong(sizeColumn)

                    audioList.add(
                        AudioEntity(
                            title = title,
                            path = path,
                            duration = duration,
                            artist = artist,
                            album = album,
                            albumId = albumId,
                            size = size
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        audioList
    }
}
