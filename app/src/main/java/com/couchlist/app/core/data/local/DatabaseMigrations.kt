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

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Rebuild media_items to v4 schema (source, category, external_id, artwork_uri,
        // backdrop_uri, description) and extract runtime into video_metadata.
        // Full rebuild because ALTER TABLE RENAME COLUMN requires SQLite >= 3.25
        // (API 30+) but minSdk is 26.
        //
        // Stage all tables, copy into the new graph, then drop staging tables
        // child-first. This remains safe with Room's foreign keys enabled.

        // 1. Rename every table to _v3 so data is staged for copy.
        db.execSQL("ALTER TABLE media_items RENAME TO media_items_v3")
        db.execSQL("ALTER TABLE seasons RENAME TO seasons_v3")
        db.execSQL("ALTER TABLE episodes RENAME TO episodes_v3")
        db.execSQL("ALTER TABLE log_entries RENAME TO log_entries_v3")
        db.execSQL("ALTER TABLE library_items RENAME TO library_items_v3")
        db.execSQL("ALTER TABLE lists RENAME TO lists_v3")
        db.execSQL("ALTER TABLE media_list_joins RENAME TO media_list_joins_v3")
        db.execSQL("ALTER TABLE watch_provider_cache RENAME TO watch_provider_cache_v3")

        // 2. Create all v4 tables.
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS media_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                source TEXT NOT NULL,
                category TEXT NOT NULL,
                external_id TEXT NOT NULL,
                title TEXT NOT NULL,
                original_title TEXT,
                description TEXT,
                artwork_uri TEXT,
                backdrop_uri TEXT,
                release_date TEXT,
                original_language TEXT,
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
            """
            CREATE TABLE IF NOT EXISTS video_metadata (
                media_id INTEGER NOT NULL PRIMARY KEY,
                runtime_minutes INTEGER,
                FOREIGN KEY (media_id) REFERENCES media_items (id)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS seasons (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                media_id INTEGER NOT NULL,
                season_number INTEGER NOT NULL,
                name TEXT NOT NULL,
                overview TEXT,
                artwork_uri TEXT,
                air_date TEXT,
                episode_count INTEGER NOT NULL,
                last_refreshed_at INTEGER,
                FOREIGN KEY (media_id) REFERENCES media_items (id)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
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
                artwork_uri TEXT,
                air_date TEXT,
                runtime_minutes INTEGER,
                FOREIGN KEY (media_id) REFERENCES media_items (id)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY (season_id) REFERENCES seasons (id)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
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

        // 3. Copy data from _v3 tables into v4 tables.
        //    media_items_v3 is still alive here so video_metadata INSERT can read runtime_minutes.
        db.execSQL(
            """
            INSERT INTO media_items (
                id, source, category, external_id, title, original_title,
                description, artwork_uri, backdrop_uri, release_date,
                original_language, external_rating, external_vote_count,
                genres, last_refreshed_at, created_at, updated_at
            )
            SELECT
                id,
                'tmdb',
                media_type,
                CAST(tmdb_id AS TEXT),
                title,
                original_title,
                overview,
                CASE
                    WHEN poster_path IS NULL THEN NULL
                    WHEN poster_path LIKE 'http://%' OR poster_path LIKE 'https://%' THEN poster_path
                    ELSE 'https://image.tmdb.org/t/p/w500' || poster_path
                END,
                CASE
                    WHEN backdrop_path IS NULL THEN NULL
                    WHEN backdrop_path LIKE 'http://%' OR backdrop_path LIKE 'https://%' THEN backdrop_path
                    ELSE 'https://image.tmdb.org/t/p/w780' || backdrop_path
                END,
                release_date,
                original_language,
                external_rating,
                external_vote_count,
                genres,
                last_refreshed_at,
                created_at,
                updated_at
            FROM media_items_v3
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO video_metadata (media_id, runtime_minutes)
            SELECT id, runtime_minutes FROM media_items_v3
            WHERE runtime_minutes IS NOT NULL
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO seasons (
                id, media_id, season_number, name, overview, artwork_uri,
                air_date, episode_count, last_refreshed_at
            )
            SELECT
                id, media_id, season_number, name, overview,
                CASE
                    WHEN poster_path IS NULL THEN NULL
                    WHEN poster_path LIKE 'http://%' OR poster_path LIKE 'https://%' THEN poster_path
                    ELSE 'https://image.tmdb.org/t/p/w500' || poster_path
                END,
                air_date, episode_count, last_refreshed_at
            FROM seasons_v3
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO episodes (
                id, media_id, season_id, season_number, episode_number,
                title, overview, artwork_uri, air_date, runtime_minutes
            )
            SELECT
                id, media_id, season_id, season_number, episode_number,
                title, overview,
                CASE
                    WHEN still_path IS NULL THEN NULL
                    WHEN still_path LIKE 'http://%' OR still_path LIKE 'https://%' THEN still_path
                    ELSE 'https://image.tmdb.org/t/p/w300' || still_path
                END,
                air_date, runtime_minutes
            FROM episodes_v3
            """.trimIndent(),
        )
        db.execSQL("INSERT INTO log_entries SELECT * FROM log_entries_v3")
        db.execSQL("INSERT INTO library_items SELECT * FROM library_items_v3")
        db.execSQL("INSERT INTO lists SELECT * FROM lists_v3")
        db.execSQL("INSERT INTO media_list_joins SELECT * FROM media_list_joins_v3")
        db.execSQL("INSERT INTO watch_provider_cache SELECT * FROM watch_provider_cache_v3")

        // 4. Drop all _v3 staging tables (child-first, parent-last).
        db.execSQL("DROP TABLE IF EXISTS watch_provider_cache_v3")
        db.execSQL("DROP TABLE IF EXISTS media_list_joins_v3")
        db.execSQL("DROP TABLE IF EXISTS lists_v3")
        db.execSQL("DROP TABLE IF EXISTS library_items_v3")
        db.execSQL("DROP TABLE IF EXISTS log_entries_v3")
        db.execSQL("DROP TABLE IF EXISTS episodes_v3")
        db.execSQL("DROP TABLE IF EXISTS seasons_v3")
        db.execSQL("DROP TABLE IF EXISTS media_items_v3")

        // 5. Create indices.
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_media_items_source_category_external_id " +
                "ON media_items (source, category, external_id)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_seasons_media_id ON seasons (media_id)")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_seasons_media_id_season_number " +
                "ON seasons (media_id, season_number)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_episodes_media_id ON episodes (media_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_episodes_season_id ON episodes (season_id)")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_episodes_season_id_episode_number " +
                "ON episodes (season_id, episode_number)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_log_entries_media_id ON log_entries (media_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_log_entries_episode_id ON log_entries (episode_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_log_entries_date ON log_entries (date)")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_library_items_media_id " +
                "ON library_items (media_id)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_library_items_status ON library_items (status)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_lists_cover_media_id ON lists (cover_media_id)")
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
            "CREATE INDEX IF NOT EXISTS index_watch_provider_cache_media_id " +
                "ON watch_provider_cache (media_id)",
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_watch_provider_cache_media_id_region " +
                "ON watch_provider_cache (media_id, region)",
        )

        // 6. Infer a COMPLETED log entry for library items that have status COMPLETED
        //    but no COMPLETED log entry and no completed_at timestamp.
        //    This preserves the "completed" history for legacy v1→v3 migrated rows
        //    while skipping items that already have a completion record.
        db.execSQL(
            """
            INSERT INTO log_entries (media_id, episode_id, action, date, personal_rating, notes, created_at, updated_at)
            SELECT li.media_id, NULL, 'COMPLETED',
                COALESCE(li.completed_at, li.updated_at, li.added_at),
                NULL, NULL,
                COALESCE(li.completed_at, li.updated_at, li.added_at),
                COALESCE(li.completed_at, li.updated_at, li.added_at)
            FROM library_items li
            WHERE li.status = 'COMPLETED'
            AND li.completed_at IS NULL
            AND NOT EXISTS (
                SELECT 1 FROM log_entries le
                WHERE le.media_id = li.media_id
                AND le.action = 'COMPLETED'
            )
            """.trimIndent(),
        )

        assertNoForeignKeyViolations(db)
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS list_groups (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                sort_order INTEGER NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            "ALTER TABLE lists ADD COLUMN group_id INTEGER " +
                "REFERENCES list_groups(id) ON UPDATE NO ACTION ON DELETE SET NULL",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_lists_group_id ON lists (group_id)")

        // Legacy imports could create list memberships without a LibraryItem.
        // Backfill those rows before making LibraryItem the membership parent.
        db.execSQL(
            """
            INSERT INTO library_items (
                media_id, status, progress, personal_rating, favorite, notes,
                added_at, started_at, completed_at, updated_at
            )
            SELECT joins.media_id, 'BACKLOG', NULL, NULL, 0, NULL,
                MIN(joins.added_at), NULL, NULL, MIN(joins.added_at)
            FROM media_list_joins joins
            LEFT JOIN library_items ON library_items.media_id = joins.media_id
            WHERE library_items.id IS NULL
            GROUP BY joins.media_id
            """.trimIndent(),
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS media_list_joins_v5 (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                list_id INTEGER NOT NULL,
                library_item_id INTEGER NOT NULL,
                added_at INTEGER NOT NULL,
                FOREIGN KEY (list_id) REFERENCES lists (id)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY (library_item_id) REFERENCES library_items (id)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO media_list_joins_v5 (id, list_id, library_item_id, added_at)
            SELECT joins.id, joins.list_id, library_items.id, joins.added_at
            FROM media_list_joins joins
            INNER JOIN library_items ON library_items.media_id = joins.media_id
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE media_list_joins")
        db.execSQL("ALTER TABLE media_list_joins_v5 RENAME TO media_list_joins")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_media_list_joins_list_id " +
                "ON media_list_joins (list_id)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_media_list_joins_library_item_id " +
                "ON media_list_joins (library_item_id)",
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_media_list_joins_list_id_library_item_id " +
                "ON media_list_joins (list_id, library_item_id)",
        )

        val now = System.currentTimeMillis()
        db.execSQL(
            """
            INSERT INTO lists (
                name, description, type, cover_media_id, is_pinned, sort_order,
                smart_filter_json, created_at, updated_at, group_id
            )
            SELECT 'The Pile', NULL, 'PILE', NULL, 1, 0, NULL, ?, ?, NULL
            WHERE NOT EXISTS (SELECT 1 FROM lists WHERE type = 'PILE')
            """.trimIndent(),
            arrayOf<Any?>(now, now),
        )
        db.execSQL(
            """
            UPDATE lists
            SET type = 'COLLECTION', is_pinned = 0, updated_at = ?
            WHERE type = 'PILE'
            AND id != (SELECT MIN(id) FROM lists WHERE type = 'PILE')
            """.trimIndent(),
            arrayOf<Any?>(now),
        )
        db.execSQL(
            """
            UPDATE lists
            SET name = 'The Pile', is_pinned = 1, group_id = NULL, updated_at = ?
            WHERE type = 'PILE'
            """.trimIndent(),
            arrayOf<Any?>(now),
        )
        assertNoForeignKeyViolations(db)
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

private fun assertNoForeignKeyViolations(db: SupportSQLiteDatabase) {
    db.query("PRAGMA foreign_key_check").use { cursor ->
        check(!cursor.moveToFirst()) {
            "Foreign key violation in ${cursor.getString(0)} at row ${cursor.getLong(1)}"
        }
    }
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
