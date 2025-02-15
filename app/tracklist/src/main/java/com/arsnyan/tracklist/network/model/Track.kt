package com.arsnyan.tracklist.network.model

import com.google.gson.annotations.SerializedName

data class Track(
    @SerializedName("id")
    val id: Long,
    @SerializedName("title")
    val title: String,
    @SerializedName("duration")
    val duration: Int,
    @SerializedName("preview")
    val srcUrl: String,
    @SerializedName("artist")
    val artist: Artist,
    @SerializedName("album")
    val album: Album,
    val trackSource: TrackSource = TrackSource.DEEZER
)