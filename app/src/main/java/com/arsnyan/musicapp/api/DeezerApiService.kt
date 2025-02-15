package com.arsnyan.musicapp.api

import com.arsnyan.tracklist.network.model.Track
import com.arsnyan.tracklist.network.model.TrackSource
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.lang.reflect.Type

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

    @GET("track/{id}")
    suspend fun getTrackById(@Path("id") id: Long): Response<Track>

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