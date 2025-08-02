package com.ahm.mydalil.data.local.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ahm.mydalil.data.model.Surah
import com.ahm.mydalil.util.Constants
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.IOException

@Database(entities = [Bookmark::class, VerseEntity::class, VerseFtsEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun verseDao(): VerseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    Constants.DATABASE_NAME
                )
                    // We handle migration by destroying and re-creating the database.
                    // This is fine for a development stage app where data is populated from assets.
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(context))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val context: Context
    ) : RoomDatabase.Callback() {
        // Use a custom scope to ensure population continues even if the viewmodel scope is cancelled.
        private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // When the DB is created for the first time, populate it.
            INSTANCE?.let { database ->
                applicationScope.launch {
                    populateDatabase(context, database.verseDao())
                }
            }
        }

        private suspend fun populateDatabase(context: Context, verseDao: VerseDao) {
            // Check if already populated (e.g., in a race condition).
            if (verseDao.getVerseCount() > 0) return

            val gson = Gson()
            val versesToInsert = mutableListOf<VerseEntity>()

            // Load Hadiths
            val hadithSurahs: List<Surah> = loadDataFromFile(context, gson, Constants.HADITHS_DB_FILE)
            hadithSurahs.forEach { surah ->
                surah.surahVerses.forEach { verse ->
                    versesToInsert.add(
                        VerseEntity(
                            source = "hadith",
                            surahNumber = surah.surahNumber,
                            surahName = surah.surahName,
                            verseNumber = verse.verseNumber,
                            verseText = verse.verseText
                        )
                    )
                }
            }

            // Load Verses
            val verseSurahs: List<Surah> = loadDataFromFile(context, gson, Constants.VERSES_DB_FILE)
            verseSurahs.forEach { surah ->
                surah.surahVerses.forEach { verse ->
                    versesToInsert.add(
                        VerseEntity(
                            source = "verse",
                            surahNumber = surah.surahNumber,
                            surahName = surah.surahName,
                            verseNumber = verse.verseNumber,
                            verseText = verse.verseText
                        )
                    )
                }
            }

            // Insert all collected data in a single transaction.
            verseDao.insertAll(versesToInsert)
        }

        private fun loadDataFromFile(context: Context, gson: Gson, fileName: String): List<Surah> {
            val surahListType = object : TypeToken<List<Surah>>() {}.type
            return try {
                context.assets.open(fileName).bufferedReader().use { reader ->
                    gson.fromJson(reader, surahListType) ?: emptyList()
                }
            } catch (e: IOException) {
                // Log error
                emptyList()
            }
        }
    }
}