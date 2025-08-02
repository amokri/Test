package com.ahm.mydalil.di

import android.content.Context
import com.ahm.mydalil.data.local.datastore.UserPreferences
import com.ahm.mydalil.data.local.room.AppDatabase
import com.ahm.mydalil.data.repository.VerseRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideUserPreferences(@ApplicationContext context: Context): UserPreferences {
        return UserPreferences(context)
    }

    @Provides
    @Singleton
    fun provideVerseRepository(
        @ApplicationContext context: Context,
        db: AppDatabase
    ): VerseRepository {
        // The getInstance method already handles the logic, we just use it here
        return VerseRepository.getInstance(context)
    }
}