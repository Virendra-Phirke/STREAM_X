package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.AudioEntity
import com.example.data.model.PlaybackHistoryEntity
import com.example.data.model.PlaylistEntity
import com.example.data.model.VideoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Query("SELECT * FROM videos")
    fun getAllVideos(): Flow<List<VideoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: VideoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideos(videos: List<VideoEntity>)

    @Query("SELECT * FROM videos WHERE id = :id LIMIT 1")
    suspend fun getVideoById(id: Int): VideoEntity?

    @Query("DELETE FROM videos")
    suspend fun deleteAll()
}

@Dao
interface AudioDao {
    @Query("SELECT * FROM audio")
    fun getAllAudio(): Flow<List<AudioEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudio(audio: AudioEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAudio(audio: List<AudioEntity>)

    @androidx.room.Update
    suspend fun updateAudio(audio: AudioEntity)

    @Query("UPDATE audio SET isLiked = :isLiked WHERE path = :path")
    suspend fun updateLikedState(path: String, isLiked: Boolean)

    @Query("SELECT * FROM audio WHERE isLiked = 1")
    fun getLikedAudio(): Flow<List<AudioEntity>>

    @Query("DELETE FROM audio")
    suspend fun deleteAll()
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Int)

    @Query("UPDATE playlists SET name = :name WHERE id = :id")
    suspend fun updatePlaylistName(id: Int, name: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistAudioItem(crossRef: com.example.data.model.PlaylistAudioCrossRef)

    @Query("DELETE FROM playlist_audio_cross_ref WHERE playlistId = :playlistId AND audioPath = :audioPath")
    suspend fun deletePlaylistAudioItem(playlistId: Int, audioPath: String)

    @Query("SELECT audio.* FROM audio INNER JOIN playlist_audio_cross_ref ON audio.path = playlist_audio_cross_ref.audioPath WHERE playlist_audio_cross_ref.playlistId = :playlistId")
    fun getAudiosForPlaylist(playlistId: Int): Flow<List<AudioEntity>>
}

@Dao
interface PlaybackHistoryDao {
    @Query("SELECT position FROM playback_history WHERE mediaPath = :mediaPath AND mediaType = :mediaType LIMIT 1")
    suspend fun getPosition(mediaPath: String, mediaType: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: PlaybackHistoryEntity)
}
