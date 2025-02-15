package com.arsnyan.musicapp.api

import com.arsnyan.tracklist.network.model.Track
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class DeezerChartResponse(
    @SerializedName("tracks")
    val tracks: TrackList
)

data class TrackList(
    @SerializedName("data")
    val data: List<Track>
)

interface DeezerApiService {
    @GET("/chart")
    suspend fun getTracks(): Response<DeezerChartResponse>

    @GET("/search")
    suspend fun searchTracks(@Query("q") query: String): Response<TrackList>

    @GET("/track")
    suspend fun getTrackById(@Query("id") id: Int): Response<Track>

    companion object {
        private const val BASE_URL = "https://api.deezer.com/"
        fun create(): DeezerApiService {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(DeezerApiService::class.java)
        }
    }
}