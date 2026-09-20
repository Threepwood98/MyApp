package com.couchlist.app.core.data.local

import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_3 = object : Migration(1, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        createVersionThreeTables(db)

        db.execSQL(
            """
            INSERT INTO media_items (
                media_type, tmdb_id, title, original_title, overview, poster_path,
                backdrop_path, release_date, original_language, runtime_minutes,
                external_rating, external_vote_count, genres, last_refreshed_at,
                created_at, updated_at
            )
            SELECT
                media_type, tmdb_id, title, NULL, NULL, poster_path,
                NULL, NULL, NULL, NULL, 0.0, 0, NULL, NULL, added_at, added_at
            FROM watchlist_items
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO library_items (
                media_id, status, progress, personal_rating, favorite, notes,
                added_at, started_at, completed_at, updated_at
            )
            SELECT
                media_items.id,
                CASE watchlist_items.status
                    WHEN 'WATCHLIST' THEN 'BACKLOG'
                    WHEN 'WATCHING' THEN 'WATCHING'
                    WHEN 'WATCHED' THEN 'COMPLETED'
                    ELSE 'BACKLOG'
                END,
                NULL, NULL, 0, NULL, watchlist_items.added_at, NULL, NULL,
                watchlist_items.added_at
            FROM watchlist_items
            INNER JOIN media_items
                ON media_items.tmdb_id = watchlist_items.tmdb_id
                AND media_items.media_type = watchlist_items.media_type
            """.trimIndent(),
        )

        val now = System.currentTimeMillis()
        seedDefaultLists(db, now)
        db.execSQL(
            """
            INSERT INTO media_list_joins (list_id, media_id, added_at)
            SELECT lists.id, media_items.id, watchlist_items.added_at
            FROM watchlist_items
            INNER JOIN media_items
                ON media_items.tmdb_id = watchlist_items.tmdb_id
                AND media_items.media_type = watchlist_items.media_type
            INNER JOIN lists
                ON lists.name = 'Watchlist' AND lists.type = 'TODO'
            WHERE watchlist_items.status = 'WATCHLIST'
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE watchlist_items")
    }
}

val SEED_DEFAULT_LISTS_CALLBACK = object : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        seedDefaultLists(db, System.currentTimeMillis())
    }
}

private fun seedDefaultLists(db: SupportSQLiteDatabase, now: Long) {
    db.execSQL(
        """
        INSERT INTO lists (
            name, description, type, cover_media_id, is_pinned, sort_order,
            smart_filter_json, created_at, updated_at
        ) VALUES (?, NULL, 'TODO', NULL, 1, 0, NULL, ?, ?)
        """.trimIndent(),
        arrayOf<Any?>("Watchlist", now, now),
    )
    db.execSQL(
        """
        INSERT INTO lists (
            name, description, type, cover_media_id, is_pinned, sort_order,
            smart_filter_json, created_at, updated_at
        ) VALUES (?, NULL, 'PILE', NULL, 1, 1, NULL, ?, ?)
        """.trimIndent(),
        arrayOf<Any?>("The Pile", now, now),
    )
}

private fun createVersionThreeTables(db: SupportSQLiteDatabase) {
    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS media_items (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            media_type TEXT NOT NULL,
            tmdb_id INTEGER NOT NULL,
            title TEXT NOT NULL,
            original_title TEXT,
            overview TEXT,
            poster_path TEXT,
            backdrop_path TEXT,
            release_date TEXT,
            original_language TEXT,
            runtime_minutes INTEGER,
            external_rating REAL NOT NULL,
            external_vote_count INTEGER NOT NULL,
            genres TEXT,
            last_refreshed_at INTEGER,
            created_at INTEGER NOT NULL,
            updated_at INTEGER NOT NULL
        )
        """.trimIndent(),
    )
    db.execSQL(
        "CREATE UNIQUE INDEX IF NOT EXISTS index_media_items_media_type_tmdb_id " +
            "ON media_items (media_type, tmdb_id)",
    )
    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS library_items (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            media_id INTEGER NOT NULL,
            status TEXT NOT NULL,
            progress REAL,
            personal_rating INTEGER,
            favorite INTEGER NOT NULL,
            notes TEXT,
            added_at INTEGER NOT NULL,
            started_at INTEGER,
            completed_at INTEGER,
            updated_at INTEGER NOT NULL,
            FOREIGN KEY (media_id) REFERENCES media_items (id)
                ON UPDATE NO ACTION ON DELETE CASCADE
        )
        """.trimIndent(),
    )
    db.execSQL(
        "CREATE UNIQUE INDEX IF NOT EXISTS index_library_items_media_id " +
            "ON library_items (media_id)",
    )
    db.execSQL(
        "CREATE INDEX IF NOT EXISTS index_library_items_status ON library_items (status)",
    )
    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS lists (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            name TEXT NOT NULL,
            description TEXT,
            type TEXT NOT NULL,
            cover_media_id INTEGER,
            is_pinned INTEGER NOT NULL,
            sort_order INTEGER NOT NULL,
            smart_filter_json TEXT,
            created_at INTEGER NOT NULL,
            updated_at INTEGER NOT NULL,
            FOREIGN KEY (cover_media_id) REFERENCES media_items (id)
                ON UPDATE NO ACTION ON DELETE SET NULL
        )
        """.trimIndent(),
    )
    db.execSQL(
        "CREATE INDEX IF NOT EXISTS index_lists_cover_media_id ON lists (cover_media_id)",
    )
    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS media_list_joins (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            list_id INTEGER NOT NULL,
            media_id INTEGER NOT NULL,
            added_at INTEGER NOT NULL,
            FOREIGN KEY (list_id) REFERENCES lists (id)
                ON UPDATE NO ACTION ON DELETE CASCADE,
            FOREIGN KEY (media_id) REFERENCES media_items (id)
                ON UPDATE NO ACTION ON DELETE CASCADE
        )
        """.trimIndent(),
    )
    db.execSQL(
        "CREATE INDEX IF NOT EXISTS index_media_list_joins_list_id " +
            "ON media_list_joins (list_id)",
    )
    db.execSQL(
        "CREATE INDEX IF NOT EXISTS index_media_list_joins_media_id " +
            "ON media_list_joins (media_id)",
    )
    db.execSQL(
        "CREATE UNIQUE INDEX IF NOT EXISTS index_media_list_joins_list_id_media_id " +
            "ON media_list_joins (list_id, media_id)",
    )
    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS seasons (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            media_id INTEGER NOT NULL,
            season_number INTEGER NOT NULL,
            name TEXT NOT NULL,
            overview TEXT,
            poster_path TEXT,
            air_date TEXT,
            episode_count INTEGER NOT NULL,
            last_refreshed_at INTEGER,
            FOREIGN KEY (media_id) REFERENCES media_items (id)
                ON UPDATE NO ACTION ON DELETE CASCADE
        )
        """.trimIndent(),
    )
    db.execSQL("CREATE INDEX IF NOT EXISTS index_seasons_media_id ON seasons (media_id)")
    db.execSQL(
        "CREATE UNIQUE INDEX IF NOT EXISTS index_seasons_media_id_season_number " +
            "ON seasons (media_id, season_number)",
    )
    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS episodes (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            media_id INTEGER NOT NULL,
            season_id INTEGER NOT NULL,
            season_number INTEGER NOT NULL,
            episode_number INTEGER NOT NULL,
            title TEXT NOT NULL,
            overview TEXT,
            still_path TEXT,
            air_date TEXT,
            runtime_minutes INTEGER,
            FOREIGN KEY (media_id) REFERENCES media_items (id)
                ON UPDATE NO ACTION ON DELETE CASCADE,
            FOREIGN KEY (season_id) REFERENCES seasons (id)
                ON UPDATE NO ACTION ON DELETE CASCADE
        )
        """.trimIndent(),
    )
    db.execSQL("CREATE INDEX IF NOT EXISTS index_episodes_media_id ON episodes (media_id)")
    db.execSQL("CREATE INDEX IF NOT EXISTS index_episodes_season_id ON episodes (season_id)")
    db.execSQL(
        "CREATE UNIQUE INDEX IF NOT EXISTS index_episodes_season_id_episode_number " +
            "ON episodes (season_id, episode_number)",
    )
    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS log_entries (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            media_id INTEGER NOT NULL,
            episode_id INTEGER,
            action TEXT NOT NULL,
            date INTEGER NOT NULL,
            personal_rating INTEGER,
            notes TEXT,
            created_at INTEGER NOT NULL,
            updated_at INTEGER NOT NULL,
            FOREIGN KEY (media_id) REFERENCES media_items (id)
                ON UPDATE NO ACTION ON DELETE CASCADE,
            FOREIGN KEY (episode_id) REFERENCES episodes (id)
                ON UPDATE NO ACTION ON DELETE SET NULL
        )
        """.trimIndent(),
    )
    db.execSQL("CREATE INDEX IF NOT EXISTS index_log_entries_media_id ON log_entries (media_id)")
    db.execSQL("CREATE INDEX IF NOT EXISTS index_log_entries_episode_id ON log_entries (episode_id)")
    db.execSQL("CREATE INDEX IF NOT EXISTS index_log_entries_date ON log_entries (date)")
    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS watch_provider_cache (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            media_id INTEGER NOT NULL,
            region TEXT NOT NULL,
            link TEXT,
            providers_json TEXT,
            refreshed_at INTEGER,
            FOREIGN KEY (media_id) REFERENCES media_items (id)
                ON UPDATE NO ACTION ON DELETE CASCADE
        )
        """.trimIndent(),
    )
    db.execSQL(
        "CREATE INDEX IF NOT EXISTS index_watch_provider_cache_media_id " +
            "ON watch_provider_cache (media_id)",
    )
    db.execSQL(
        "CREATE UNIQUE INDEX IF NOT EXISTS index_watch_provider_cache_media_id_region " +
            "ON watch_provider_cache (media_id, region)",
    )
}
