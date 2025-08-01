package com.ahm.mydalil.data.repository

import android.content.Context
import android.util.Log
import com.ahm.mydalil.data.local.room.AppDatabase
import com.ahm.mydalil.data.local.room.Bookmark
import com.ahm.mydalil.data.local.room.toBookmark
import com.ahm.mydalil.data.model.Surah
import com.ahm.mydalil.data.model.Verse
import com.ahm.mydalil.util.Constants
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException

class VerseRepository private constructor(
    private val context: Context,
    private val db: AppDatabase
) {
    private val gson = Gson()
    private val surahListType = object : TypeToken<List<Surah>>() {}.type

    // Data is loaded once and stored, improving performance significantly.
    private val hadithData: List<Surah> by lazy { loadDataFromFile(Constants.HADITHS_DB_FILE) }
    private val verseData: List<Surah> by lazy { loadDataFromFile(Constants.VERSES_DB_FILE) }

    //    val allSurahNames: List<String> by lazy {
//        (hadithData.map { it.surahName } + verseData.map { it.surahName }).distinct().sorted()
//    }
    val allSurahNames: List<String> by lazy {
        (hadithData + verseData)
            .distinctBy { it.surahName }
            .sortedBy { it.surahNumber }
            .map { it.surahName }
    }

    val hadithSurahNames: Set<String> by lazy { hadithData.map { it.surahName }.toSet() }
    val verseSurahNames: Set<String> by lazy { verseData.map { it.surahName }.toSet() }

    private fun loadDataFromFile(fileName: String): List<Surah> {
        return try {
            context.assets.open(fileName).bufferedReader().use { reader ->
                gson.fromJson(reader, surahListType) ?: emptyList()
            }
        } catch (e: IOException) {
            Log.e("VerseRepository", "Error reading data from assets: $fileName", e)
            emptyList()
        } catch (e: Exception) {
            Log.e("VerseRepository", "Error parsing data: $fileName", e)
            emptyList()
        }
    }

    data class PagedResults(
        val items: List<VerseSearchResult>,
        val totalCount: Int,
        val hasNextPage: Boolean
    )

    data class VerseSearchResult(val verse: Verse, val surahName: String, val surahNumber: Int) {
        /** Provides a stable, unique ID for a search result, used for keys and bookmarks. */
        val id: Int get() = "$surahNumber.${verse.verseNumber} $surahName".hashCode()
    }

    suspend fun search(
        query: String,
        surahFilters: Set<String>,
        allWordsRequired: Boolean,
        page: Int,
        pageSize: Int,
        loadHadiths: Boolean,
        loadVerses: Boolean
    ): PagedResults = withContext(Dispatchers.IO) {
        val searchTerms = query.split(Regex("\\s+")).filter { it.isNotBlank() }.map { it.lowercase() }

        val activeData = when {
            loadHadiths && loadVerses -> hadithData + verseData
            loadHadiths -> hadithData
            loadVerses -> verseData
            else -> emptyList()
        }

        val filteredSurahs = if (surahFilters.isNotEmpty()) {
            activeData.filter { surah -> surahFilters.contains(surah.surahName) }
        } else {
            activeData
        }

        if (searchTerms.isEmpty()) {
            // If query is empty, return all verses from filtered surahs, paginated.
            val allResults = filteredSurahs.flatMap { surah ->
                surah.surahVerses.map { verse ->
                    VerseSearchResult(verse, surah.surahName, surah.surahNumber)
                }
            }
            val start = (page * pageSize).coerceAtMost(allResults.size)
            val end = (start + pageSize).coerceAtMost(allResults.size)
            return@withContext PagedResults(
                items = allResults.subList(start, end),
                totalCount = allResults.size,
                hasNextPage = end < allResults.size
            )
        }

        val allResults = mutableListOf<VerseSearchResult>()
        for (surah in filteredSurahs) {
            for (verse in surah.surahVerses) {
                val textToSearch = verse.verseText.lowercase()
                val found = if (allWordsRequired) {
                    searchTerms.all { textToSearch.contains(it) }
                } else {
                    searchTerms.any { textToSearch.contains(it) }
                }

                if (found) {
                    allResults.add(VerseSearchResult(verse, surah.surahName, surah.surahNumber))
                }
            }
        }

        val start = (page * pageSize).coerceAtMost(allResults.size)
        val end = (start + pageSize).coerceAtMost(allResults.size)

        PagedResults(
            items = allResults.subList(start, end),
            totalCount = allResults.size,
            hasNextPage = end < allResults.size
        )
    }

    suspend fun getVersesForSurah(surahName: String): List<VerseSearchResult> = withContext(Dispatchers.IO) {
        val allData = hadithData + verseData
        allData
            .filter { it.surahName.equals(surahName, ignoreCase = true) }
            .flatMap { surah ->
                surah.surahVerses.map { verse ->
                    VerseSearchResult(verse, surah.surahName, surah.surahNumber)
                }
            }
    }

    // --- Bookmark Functions ---

    fun getAllBookmarks(): Flow<List<Bookmark>> = db.bookmarkDao().getAllBookmarks()

    fun getBookmarkedIds(): Flow<Set<Int>> = db.bookmarkDao().getAllBookmarkedIds().map { it.toSet() }

    suspend fun toggleBookmark(result: VerseSearchResult, query: String) = withContext(Dispatchers.IO) {
        val existing = db.bookmarkDao().getBookmarkByVerseId(result.id)
        if (existing != null) {
            db.bookmarkDao().deleteBookmark(existing)
        } else {
            db.bookmarkDao().insertBookmark(result.toBookmark(query))
        }
    }

    // --- Companion Object for Singleton Pattern ---

    companion object {
        @Volatile
        private var INSTANCE: VerseRepository? = null

        fun getInstance(context: Context): VerseRepository {
            return INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getDatabase(context)
                VerseRepository(context.applicationContext, db).also { INSTANCE = it }
            }
        }
    }
}