package com.arsnyan.tracklist.network.repository

import com.arsnyan.tracklist.network.model.Track

interface TrackDataSource {
    suspend fun getAllTracks(): Result<List<Track>>
    suspend fun searchTracks(query: String): Result<List<Track>>
    suspend fun getTrackById(id: Long): Result<Track>
}