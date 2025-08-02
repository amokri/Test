package com.ahm.mydalil.data.local.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

/**
 * Represents a single verse or hadith entry in the database.
 * This is the main content entity.
 */
@Entity(tableName = "verses")
data class VerseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val source: String, // "hadith" or "verse"
    val surahNumber: Int,
    val surahName: String,
    val verseNumber: Int,
    val verseText: String
)

/**
 * FTS5 virtual table for full-text searching on the verseText column.
 * It's linked to the VerseEntity table for efficient lookups.
 */
@Fts4(contentEntity = VerseEntity::class)
@Entity(tableName = "verses_fts")
data class VerseFtsEntity(
    // We only need to specify the column we want to index for FTS.
    // The name must match the column in the contentEntity.
    @ColumnInfo(name = "verseText")
    val verseText: String
)