package com.arsnyan.musicapp.api

import com.arsnyan.tracklist.network.model.Track
import com.arsnyan.tracklist.network.repository.TrackDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DeezerTrackDataSource @Inject constructor(private val apiService: DeezerApiService)
    : TrackDataSource {
    override suspend fun getAllTracks(): Result<List<Track>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getTracks()
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it.tracks.data)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("Response failed with code: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchTracks(query: String): Result<List<Track>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.searchTracks(query)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it.data)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("Response failed with code: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTrackById(id: Int): Result<Track> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getTrackById(id)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("Response failed with code: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}