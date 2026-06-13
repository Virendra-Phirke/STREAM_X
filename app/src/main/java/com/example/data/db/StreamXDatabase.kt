package com.example.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.data.dao.AudioDao
import com.example.data.dao.PlaybackHistoryDao
import com.example.data.dao.PlaylistDao
import com.example.data.dao.VideoDao
import com.example.data.model.AudioEntity
import com.example.data.model.PlaybackHistoryEntity
import com.example.data.model.PlaylistEntity
import com.example.data.model.VideoEntity

import com.example.data.model.PlaylistAudioCrossRef

@Database(
    entities = [VideoEntity::class, AudioEntity::class, PlaylistEntity::class, PlaybackHistoryEntity::class, PlaylistAudioCrossRef::class],
    version = 4,
    exportSchema = false
)
abstract class StreamXDatabase : RoomDatabase() {
    abstract val videoDao: VideoDao
    abstract val audioDao: AudioDao
    abstract val playlistDao: PlaylistDao
    abstract val playbackHistoryDao: PlaybackHistoryDao
}
