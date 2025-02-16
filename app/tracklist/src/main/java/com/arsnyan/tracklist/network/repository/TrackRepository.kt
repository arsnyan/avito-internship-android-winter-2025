package com.arsnyan.tracklist.network.repository

import com.arsnyan.tracklist.network.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TrackRepository(
    private val dataSource: TrackDataSource
) {
    suspend fun getTracks(): Result<List<Track>> {
        return dataSource.getAllTracks()
    }

    suspend fun searchTracks(query: String): Result<List<Track>?> {
        return dataSource.searchTracks(query)
    }

    suspend fun getTrackById(id: Long): Result<Track> = withContext(Dispatchers.IO) {
        dataSource.getTrackById(id)
    }
}