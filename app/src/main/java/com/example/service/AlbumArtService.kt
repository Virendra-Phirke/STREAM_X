package com.example.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.HttpURLConnection

object AlbumArtService {
    suspend fun fetchAlbumArtUrl(artist: String, album: String, title: String): String? = withContext(Dispatchers.IO) {
        try {
            val query = if (album.isNotBlank() && album != "<unknown>") {
                "$artist $album"
            } else {
                "$artist $title"
            }
            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            val urlString = "https://itunes.apple.com/search?term=$encodedQuery&media=music&entity=song&limit=1"
            
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val results = json.optJSONArray("results")
                if (results != null && results.length() > 0) {
                    val track = results.getJSONObject(0)
                    val artworkUrl = track.optString("artworkUrl100")
                    if (artworkUrl.isNotEmpty()) {
                        // Replace 100x100 with 600x600 for higher resolution
                        return@withContext artworkUrl.replace("100x100bb", "600x600bb")
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }
}
