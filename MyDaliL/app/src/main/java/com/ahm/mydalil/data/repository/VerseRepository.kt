package com.ahm.mydalil.data.repository

import com.ahm.mydalil.data.local.room.Bookmark
import com.ahm.mydalil.data.local.room.BookmarkDao
import com.ahm.mydalil.data.local.room.VerseDao
import com.ahm.mydalil.data.local.room.VerseEntity
import com.ahm.mydalil.data.local.room.toBookmark
import com.ahm.mydalil.data.model.Verse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VerseRepository @Inject constructor(
    private val verseDao: VerseDao,
    private val bookmarkDao: BookmarkDao
) {

    // --- Data Flows from Database ---
    val allSurahNames: Flow<List<String>> = verseDao.getAllSurahNames()
    val hadithSurahNames: Flow<List<String>> = verseDao.getHadithSurahNames()
    val verseSurahNames: Flow<List<String>> = verseDao.getVerseSurahNames()

    // --- Search Result Data Classes ---
    data class PagedResults(
        val items: List<VerseSearchResult>,
        val totalCount: Int,
        val hasNextPage: Boolean
    )

    data class VerseSearchResult(val verse: Verse, val surahName: String, val surahNumber: Int, val source: String) {
        /** Provides a stable, unique ID for a search result, used for keys and bookmarks. */
        val id: Int get() = "$source-$surahNumber.${verse.verseNumber} $surahName".hashCode()
    }

    // --- Search Functionality ---
    suspend fun search(
        query: String,
        surahFilters: Set<String>,
        allWordsRequired: Boolean,
        page: Int,
        pageSize: Int,
        loadHadiths: Boolean,
        loadVerses: Boolean
    ): PagedResults = withContext(Dispatchers.IO) {

        val sources = mutableListOf<String>().apply {
            if (loadHadiths) add("hadith")
            if (loadVerses) add("verse")
        }

        val ftsQuery = if (query.isBlank()) {
            "*" // FTS query to match everything when the search bar is empty
        } else {
            // Sanitize query to prevent FTS errors and prepare for term processing
            val sanitizedQuery = query.replace(Regex("[^\\p{L}\\p{N}\\s*]"), " ").trim()
            val terms = sanitizedQuery.split(Regex("\\s+")).filter { it.isNotBlank() }

            if (terms.isEmpty()) {
                // Return empty if query contains only special characters
                return@withContext PagedResults(emptyList(), 0, false)
            }

            // For "all words required", FTS uses a space as an implicit AND operator.
            // For "any word", the OR operator must be explicit.
            // A wildcard (*) is added to each term for prefix matching.
            if (allWordsRequired) {
                terms.joinToString(" ") { "$it*" }
            } else {
                terms.joinToString(" OR ") { "$it*" }
            }
        }

        val offset = page * pageSize

        val applySourceFilter = sources.isNotEmpty()
        val applySurahFilter = surahFilters.isNotEmpty()

        val results = verseDao.search(
            ftsQuery = ftsQuery,
            applySourceFilter = applySourceFilter,
            sources = if (applySourceFilter) sources else listOf(""), // Pass non-empty list
            applySurahFilter = applySurahFilter,
            surahFilters = if (applySurahFilter) surahFilters else setOf(""), // Pass non-empty set
            pageSize = pageSize,
            offset = offset
        )

        val totalCount = verseDao.getSearchTotalCount(
            ftsQuery = ftsQuery,
            applySourceFilter = applySourceFilter,
            sources = if (applySourceFilter) sources else listOf(""),
            applySurahFilter = applySurahFilter,
            surahFilters = if (applySurahFilter) surahFilters else setOf("")
        )

        PagedResults(
            items = results.map { it.toVerseSearchResult() },
            totalCount = totalCount,
            hasNextPage = (offset + results.size) < totalCount
        )
    }

    suspend fun getVersesForSurah(surahName: String): List<VerseSearchResult> = withContext(Dispatchers.IO) {
        verseDao.getVersesForSurah(surahName).map { it.toVerseSearchResult() }
    }

    // --- Bookmark Functions ---
    fun getAllBookmarks(): Flow<List<Bookmark>> = bookmarkDao.getAllBookmarks()

    fun getBookmarkedIds(): Flow<Set<Int>> = bookmarkDao.getAllBookmarkedIds().map { it.toSet() }

    suspend fun toggleBookmark(result: VerseSearchResult, query: String) = withContext(Dispatchers.IO) {
        val existing = bookmarkDao.getBookmarkByVerseId(result.id)
        if (existing != null) {
            bookmarkDao.deleteBookmark(existing)
        } else {
            bookmarkDao.insertBookmark(result.toBookmark(query))
        }
    }

    // --- Mapper Function ---
    private fun VerseEntity.toVerseSearchResult(): VerseSearchResult {
        return VerseSearchResult(
            verse = Verse(verseNumber, verseText),
            surahName = surahName,
            surahNumber = surahNumber,
            source = source
        )
    }
}