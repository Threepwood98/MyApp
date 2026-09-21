package com.couchlist.app.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.couchlist.app.core.data.local.dao.LibraryItemDao
import com.couchlist.app.core.data.local.dao.LogEntryDao
import com.couchlist.app.core.data.local.dao.MediaItemDao
import com.couchlist.app.core.data.local.dao.MediaListDao
import com.couchlist.app.core.data.local.dao.TvDao
import com.couchlist.app.core.data.local.entity.EpisodeEntity
import com.couchlist.app.core.data.local.entity.LibraryItemEntity
import com.couchlist.app.core.data.local.entity.LogEntryEntity
import com.couchlist.app.core.data.local.entity.MediaItemEntity
import com.couchlist.app.core.data.local.entity.MediaListEntity
import com.couchlist.app.core.data.local.entity.MediaListJoinEntity
import com.couchlist.app.core.data.local.entity.SeasonEntity
import com.couchlist.app.core.data.local.entity.WatchProviderCacheEntity

@Database(
    entities = [
        MediaItemEntity::class,
        LibraryItemEntity::class,
        MediaListEntity::class,
        MediaListJoinEntity::class,
        SeasonEntity::class,
        EpisodeEntity::class,
        LogEntryEntity::class,
        WatchProviderCacheEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class CouchlistDatabase : RoomDatabase() {
    abstract fun mediaItemDao(): MediaItemDao

    abstract fun libraryItemDao(): LibraryItemDao

    abstract fun mediaListDao(): MediaListDao

    abstract fun logEntryDao(): LogEntryDao

    abstract fun tvDao(): TvDao
}
