package com.ahm.mydalil.data.local.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ahm.mydalil.data.repository.VerseRepository

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val verseId: Int, // Unique hashcode of the verse content
    val surahNumber: Int,
    val surahName: String,
    val verseNumber: Int,
    val verseText: String,
    val createdAt: Long = System.currentTimeMillis(),
    val query: String = "" // The search query that led to this bookmark
) {
    /** Provides a stable, unique ID for a bookmark, used for keys in LazyColumn. */
    val stableId: Int get() = verseId
}

fun VerseRepository.VerseSearchResult.toBookmark(query: String): Bookmark {
    return Bookmark(
        verseId = this.id,
        surahNumber = this.surahNumber,
        surahName = this.surahName,
        verseNumber = this.verse.verseNumber,
        verseText = this.verse.verseText,
        query = query
    )
}
