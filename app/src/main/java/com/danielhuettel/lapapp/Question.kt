package com.danielhuettel.lapapp

import com.google.gson.annotations.SerializedName

data class Question(
    @SerializedName("id") val id: String = "",
    @SerializedName("frage") val frage: String = "",
    @SerializedName("antwort") val antwort: String = "",
    @SerializedName("bilder") val bilder: List<String> = emptyList()
)
