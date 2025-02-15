package com.arsnyan.musicapp.local

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.arsnyan.tracklist.network.model.Album
import com.arsnyan.tracklist.network.model.Artist
import com.arsnyan.tracklist.network.model.Track
import com.arsnyan.tracklist.network.model.TrackSource
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

    override suspend fun getTrackById(id: Long): Result<Track> {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION // Add duration to the projection
        )

        val selection = "${MediaStore.Audio.Media._ID} = ?"
        val selectionArgs = arrayOf(id.toString())

        try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                    val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                    val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(titleColumn)
                    val artist = cursor.getString(artistColumn)
                    val albumId = cursor.getLong(albumIdColumn)
                    val data = cursor.getString(dataColumn)
                    val duration = cursor.getLong(durationColumn) // Get duration from cursor


                    val albumCoverUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/albumart"),
                        albumId
                    ).toString()

                    val artistData = Artist(name = artist)
                    val albumData = Album(
                        title = "Unknown album", // You might want to query for the album title
                        coverUrl = albumCoverUri,
                        coverXlUrl = albumCoverUri
                    )

                    return Result.success(
                        Track(
                            id = id,
                            title = title,
                            artist = artistData,
                            album = albumData,
                            srcUrl = data,
                            duration = duration.toInt(),
                            trackSource = TrackSource.LOCAL
                        )
                    )
                } else {
                    return Result.failure(Exception("No track found with ID: $id"))
                }
            } ?: return Result.failure(Exception("Cursor is null"))
        } catch (e: Exception) {
            Log.e("MUSICAPP", "Error fetching track by ID: ${e.message}", e)
            return Result.failure(e)
        }
    }

    private fun fetchLocalTracks(query: String = ""): Result<List<Track>> {
        val tracks = mutableListOf<Track>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION
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
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(titleColumn)
                    val artist = cursor.getString(artistColumn)
                    val albumId = cursor.getLong(albumIdColumn)
                    val data = cursor.getString(dataColumn)
                    val duration = cursor.getInt(durationColumn)

                    val albumCoverUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/albumart"),
                        albumId
                    ).toString()

                    val artistData = Artist(name = artist)
                    val albumData = Album(
                        title = "Unknown album", coverUrl = albumCoverUri,
                        coverXlUrl = albumCoverUri
                    )
                    tracks.add(
                        Track(
                            id = id,
                            title = title,
                            artist = artistData,
                            album = albumData,
                            srcUrl = data,
                            duration = duration,
                            trackSource = TrackSource.LOCAL
                        )
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