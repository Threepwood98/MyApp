package com.couchlist.app.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.couchlist.app.core.data.local.dao.WatchlistDao
import com.couchlist.app.core.data.local.entity.WatchlistEntity

@Database(
    entities = [WatchlistEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class CouchlistDatabase : RoomDatabase() {
    abstract fun watchlistDao(): WatchlistDao
}