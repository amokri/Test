package com.ahm.mydalil.data.model

import com.google.gson.annotations.SerializedName

data class Surah(
    @SerializedName("Id") val surahNumber: Int,
    @SerializedName("Name") val surahName: String,
    @SerializedName("Verses") val surahVerses: List<Verse>
)

data class Verse(
    @SerializedName("Number") val verseNumber: Int,
    @SerializedName("Ayat") val verseText: String
)
