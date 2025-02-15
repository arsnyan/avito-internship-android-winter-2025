package com.arsnyan.musicapp.local

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.arsnyan.tracklist.network.model.Album
import com.arsnyan.tracklist.network.model.Artist
import com.arsnyan.tracklist.network.model.Track
import com.arsnyan.tracklist.network.repository.TrackDataSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LocalTrackDataSource @Inject constructor(@ApplicationContext private val context: Context)
    : TrackDataSource {
    override suspend fun getAllTracks(): Result<List<Track>> = withContext(Dispatchers.IO) {
        fetchLocalTracks()
    }

    override suspend fun searchTracks(query: String): Result<List<Track>> = withContext(Dispatchers.IO) {
        fetchLocalTracks(query)
    }

    override suspend fun getTrackById(id: Int): Result<Track> {
        TODO("Not yet implemented")
    }

    private fun fetchLocalTracks(query: String = ""): Result<List<Track>> {
        val tracks = mutableListOf<Track>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATA
        )

        val selection = if (query.isNotEmpty()) {
            "${MediaStore.Audio.Media.TITLE} LIKE ? OR ${MediaStore.Audio.Media.ARTIST} LIKE ?"
        } else {
            null
        }

        val selectionArgs = if (query.isNotEmpty()) {
            arrayOf("%$query%", "%$query%")
        } else {
            null
        }

        try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                MediaStore.Audio.Media.TITLE + " ASC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(titleColumn)
                    val artist = cursor.getString(artistColumn)
                    val albumId = cursor.getLong(albumIdColumn)
                    val data = cursor.getString(dataColumn)

                    val albumCoverUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/albumart"),
                        albumId
                    ).toString()

                    val artistData = Artist(name = artist)
                    val albumData = Album(title = "Unknown album", coverUrl = albumCoverUri)
                    tracks.add(
                        Track(
                            id = id,
                            title = title,
                            artist = artistData,
                            album = albumData,
                            srcUrl = data,
                            duration = 0
                        ) // duration is unnecessary in this case, so I'd rather not bother to retrieve it here
                    )
                }
                Log.i("MUSICAPP", tracks.toString())
                return Result.success(tracks)
            } ?: return Result.failure(Exception("Cursor is null"))
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}