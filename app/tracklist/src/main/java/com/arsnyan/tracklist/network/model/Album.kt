package com.arsnyan.tracklist.network.model

import com.google.gson.annotations.SerializedName

data class Album(
    @SerializedName("title")
    val title: String,
    @SerializedName("cover")
    val coverUrl: String
)
