package com.couchlist.app.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.couchlist.app.core.data.local.CouchlistDatabase
import com.couchlist.app.core.data.local.MIGRATION_1_3
import com.couchlist.app.core.data.local.SEED_DEFAULT_LISTS_CALLBACK
import com.couchlist.app.core.data.local.dao.LibraryItemDao
import com.couchlist.app.core.data.local.dao.LogEntryDao
import com.couchlist.app.core.data.local.dao.MediaItemDao
import com.couchlist.app.core.data.local.dao.MediaListDao
import com.couchlist.app.core.data.local.dao.TvDao
import com.couchlist.app.core.data.repository.CatalogRepositoryImpl
import com.couchlist.app.core.data.repository.LibraryRepositoryImpl
import com.couchlist.app.core.data.repository.LogbookRepositoryImpl
import com.couchlist.app.core.data.repository.PreferencesSettingsRepository
import com.couchlist.app.core.data.repository.StatisticsRepositoryImpl
import com.couchlist.app.core.data.repository.TvRepositoryImpl
import com.couchlist.app.core.domain.repository.CatalogRepository
import com.couchlist.app.core.domain.repository.LibraryRepository
import com.couchlist.app.core.domain.repository.LogbookRepository
import com.couchlist.app.core.domain.repository.MediaRepository
import com.couchlist.app.core.domain.repository.SettingsRepository
import com.couchlist.app.core.domain.repository.StatisticsRepository
import com.couchlist.app.core.domain.repository.TvRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.settingsDataStore

    @Provides
    @Singleton
    fun provideSettingsRepository(dataStore: DataStore<Preferences>): SettingsRepository =
        PreferencesSettingsRepository(dataStore)

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CouchlistDatabase =
        Room.databaseBuilder(
            context,
            CouchlistDatabase::class.java,
            "couchlist.db",
        )
            .addMigrations(MIGRATION_1_3)
            .addCallback(SEED_DEFAULT_LISTS_CALLBACK)
            .build()

    @Provides
    @Singleton
    fun provideMediaItemDao(database: CouchlistDatabase): MediaItemDao = database.mediaItemDao()

    @Provides
    @Singleton
    fun provideLibraryItemDao(database: CouchlistDatabase): LibraryItemDao =
        database.libraryItemDao()

    @Provides
    @Singleton
    fun provideMediaListDao(database: CouchlistDatabase): MediaListDao = database.mediaListDao()

    @Provides
    @Singleton
    fun provideTvDao(database: CouchlistDatabase): TvDao = database.tvDao()

    @Provides
    @Singleton
    fun provideLogEntryDao(database: CouchlistDatabase): LogEntryDao = database.logEntryDao()

    @Provides
    @Singleton
    fun provideLibraryRepository(
        database: CouchlistDatabase,
        libraryItemDao: LibraryItemDao,
        mediaListDao: MediaListDao,
    ): LibraryRepository = LibraryRepositoryImpl(database, libraryItemDao, mediaListDao)

    @Provides
    @Singleton
    fun provideCatalogRepository(
        database: CouchlistDatabase,
        mediaItemDao: MediaItemDao,
        tvDao: TvDao,
        mediaRepository: MediaRepository,
    ): CatalogRepository = CatalogRepositoryImpl(database, mediaItemDao, tvDao, mediaRepository)

    @Provides
    @Singleton
    fun provideTvRepository(
        database: CouchlistDatabase,
        mediaItemDao: MediaItemDao,
        libraryItemDao: LibraryItemDao,
        tvDao: TvDao,
        mediaRepository: MediaRepository,
    ): TvRepository = TvRepositoryImpl(
        database,
        mediaItemDao,
        libraryItemDao,
        tvDao,
        mediaRepository,
    )

    @Provides
    @Singleton
    fun provideLogbookRepository(
        logEntryDao: LogEntryDao,
    ): LogbookRepository = LogbookRepositoryImpl(logEntryDao)

    @Provides
    @Singleton
    fun provideStatisticsRepository(
        logEntryDao: LogEntryDao,
        mediaItemDao: MediaItemDao,
    ): StatisticsRepository = StatisticsRepositoryImpl(logEntryDao, mediaItemDao)
}
