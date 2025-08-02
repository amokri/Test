package com.ahm.mydalil.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface VerseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(verses: List<VerseEntity>)

    @Query("SELECT COUNT(*) FROM verses")
    suspend fun getVerseCount(): Int

    @Transaction
    @Query("""
        SELECT v.* FROM verses v
        JOIN verses_fts ON v.id = verses_fts.rowid
        WHERE verses_fts MATCH :ftsQuery
        AND (:applySourceFilter = 0 OR v.source IN (:sources))
        AND (:applySurahFilter = 0 OR v.surahName IN (:surahFilters))
        ORDER BY v.source, v.surahNumber, v.verseNumber
        LIMIT :pageSize OFFSET :offset
    """)
    suspend fun search(
        ftsQuery: String,
        applySourceFilter: Boolean,
        sources: List<String>,
        applySurahFilter: Boolean,
        surahFilters: Set<String>,
        pageSize: Int,
        offset: Int
    ): List<VerseEntity>

    @Query("""
        SELECT count(*) FROM verses v
        JOIN verses_fts ON v.id = verses_fts.rowid
        WHERE verses_fts MATCH :ftsQuery
        AND (:applySourceFilter = 0 OR v.source IN (:sources))
        AND (:applySurahFilter = 0 OR v.surahName IN (:surahFilters))
    """)
    suspend fun getSearchTotalCount(
        ftsQuery: String,
        applySourceFilter: Boolean,
        sources: List<String>,
        applySurahFilter: Boolean,
        surahFilters: Set<String>
    ): Int

    @Query("SELECT DISTINCT surahName FROM verses ORDER BY surahName ASC")
    fun getAllSurahNames(): Flow<List<String>>

    @Query("SELECT DISTINCT surahName FROM verses WHERE source = 'hadith' ORDER BY surahNumber ASC")
    fun getHadithSurahNames(): Flow<List<String>>

    @Query("SELECT DISTINCT surahName FROM verses WHERE source = 'verse' ORDER BY surahNumber ASC")
    fun getVerseSurahNames(): Flow<List<String>>

    @Query("SELECT * FROM verses WHERE surahName = :surahName ORDER BY verseNumber ASC")
    suspend fun getVersesForSurah(surahName: String): List<VerseEntity>
}