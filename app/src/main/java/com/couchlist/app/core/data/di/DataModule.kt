package com.couchlist.app.core.data.di

import android.content.Context
import androidx.room.Room
import com.couchlist.app.core.data.local.CouchlistDatabase
import com.couchlist.app.core.data.local.dao.WatchlistDao
import com.couchlist.app.core.data.repository.WatchlistRepositoryImpl
import com.couchlist.app.core.domain.repository.WatchlistRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CouchlistDatabase =
        Room.databaseBuilder(
            context,
            CouchlistDatabase::class.java,
            "couchlist.db",
        ).build()

    @Provides
    @Singleton
    fun provideWatchlistDao(database: CouchlistDatabase): WatchlistDao =
        database.watchlistDao()

    @Provides
    @Singleton
    fun provideWatchlistRepository(dao: WatchlistDao): WatchlistRepository =
        WatchlistRepositoryImpl(dao)
}