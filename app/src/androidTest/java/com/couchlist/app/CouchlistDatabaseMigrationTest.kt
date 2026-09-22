package com.couchlist.app

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import com.couchlist.app.core.data.local.CouchlistDatabase
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class CouchlistDatabaseMigrationTest {

    companion object {
        private const val TEST_DB_NAME = "migration-test"
    }

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        CouchlistDatabase::class.java,
    )

    @Test
    fun migrate3To4() {
        var db = helper.createDatabase(TEST_DB_NAME, 3).apply {
            execSQL(
                """
                INSERT INTO media_items (
                    media_type, tmdb_id, title, original_title, overview, poster_path,
                    backdrop_path, release_date, original_language, runtime_minutes,
                    external_rating, external_vote_count, genres, last_refreshed_at,
                    created_at, updated_at
                ) VALUES ('MOVIE', 12345, 'Test Movie', 'Test Original', 'A overview',
                    '/poster.jpg', '/backdrop.jpg', '2024-01-01', 'en', 120,
                    8.5, 1000, 'Drama, Action', 1000000, 1000000, 1000000)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO media_items (
                    media_type, tmdb_id, title, original_title, overview, poster_path,
                    backdrop_path, release_date, original_language, runtime_minutes,
                    external_rating, external_vote_count, genres, last_refreshed_at,
                    created_at, updated_at
                ) VALUES ('TV', 67890, 'Test Show', null, null,
                    null, null, null, null, null,
                    0.0, 0, null, null, 1000000, 1000000)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO library_items (media_id, status, progress, personal_rating,
                    favorite, notes, added_at, started_at, completed_at, updated_at)
                VALUES (1, 'COMPLETED', NULL, NULL, 0, NULL, 1000000, NULL, NULL, 1000000)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO seasons (media_id, season_number, name, overview, poster_path,
                    air_date, episode_count, last_refreshed_at)
                VALUES (2, 1, 'Season 1', 'First season', '/season.jpg',
                    '2024-01-01', 10, 1000000)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO episodes (media_id, season_id, season_number, episode_number,
                    title, overview, still_path, air_date, runtime_minutes)
                VALUES (2, 1, 1, 1, 'Pilot', 'First episode', '/still.jpg', '2024-01-01', 45)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO log_entries (media_id, episode_id, action, date,
                    personal_rating, notes, created_at, updated_at)
                VALUES (2, 1, 'EPISODE_WATCHED', 1000000, NULL, NULL, 1000000, 1000000)
                """.trimIndent(),
            )
            close()
        }

        db = helper.runMigrationsAndValidate(
            TEST_DB_NAME,
            4,
            true,
            com.couchlist.app.core.data.local.MIGRATION_3_4,
        )

        val cursor = db.query("SELECT source, category, external_id, title, description, artwork_uri, backdrop_uri FROM media_items WHERE id = 1")
        assertTrue(cursor.moveToFirst())
        assertEquals("tmdb", cursor.getString(cursor.getColumnIndexOrThrow("source")))
        assertEquals("MOVIE", cursor.getString(cursor.getColumnIndexOrThrow("category")))
        assertEquals("12345", cursor.getString(cursor.getColumnIndexOrThrow("external_id")))
        assertEquals("Test Movie", cursor.getString(cursor.getColumnIndexOrThrow("title")))
        assertEquals("A overview", cursor.getString(cursor.getColumnIndexOrThrow("description")))
        assertEquals("https://image.tmdb.org/t/p/w500/poster.jpg", cursor.getString(cursor.getColumnIndexOrThrow("artwork_uri")))
        assertEquals("https://image.tmdb.org/t/p/w780/backdrop.jpg", cursor.getString(cursor.getColumnIndexOrThrow("backdrop_uri")))
        cursor.close()

        val runtimeCursor = db.query("SELECT runtime_minutes FROM video_metadata WHERE media_id = 1")
        assertTrue(runtimeCursor.moveToFirst())
        assertEquals(120, runtimeCursor.getInt(runtimeCursor.getColumnIndexOrThrow("runtime_minutes")))
        runtimeCursor.close()

        val seasonCursor = db.query("SELECT artwork_uri, name FROM seasons WHERE media_id = 2")
        assertTrue(seasonCursor.moveToFirst())
        assertEquals("Season 1", seasonCursor.getString(seasonCursor.getColumnIndexOrThrow("name")))
        assertEquals("https://image.tmdb.org/t/p/w500/season.jpg", seasonCursor.getString(seasonCursor.getColumnIndexOrThrow("artwork_uri")))
        seasonCursor.close()

        val episodeCursor = db.query("SELECT artwork_uri, runtime_minutes FROM episodes WHERE media_id = 2")
        assertTrue(episodeCursor.moveToFirst())
        assertEquals("https://image.tmdb.org/t/p/w300/still.jpg", episodeCursor.getString(episodeCursor.getColumnIndexOrThrow("artwork_uri")))
        assertEquals(45, episodeCursor.getInt(episodeCursor.getColumnIndexOrThrow("runtime_minutes")))
        episodeCursor.close()

        val libCursor = db.query("SELECT media_id, status FROM library_items WHERE media_id = 1")
        assertTrue(libCursor.moveToFirst())
        assertEquals(1, libCursor.getInt(libCursor.getColumnIndexOrThrow("media_id")))
        assertEquals("COMPLETED", libCursor.getString(libCursor.getColumnIndexOrThrow("status")))
        libCursor.close()

        val logCursor = db.query("SELECT action FROM log_entries WHERE media_id = 1 AND action = 'COMPLETED'")
        assertTrue(logCursor.moveToFirst())
        assertEquals("COMPLETED", logCursor.getString(logCursor.getColumnIndexOrThrow("action")))
        logCursor.close()
    }

    @Test
    fun migrate1To3To4To5To6() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(TEST_DB_NAME)
        val versionOneHelper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(TEST_DB_NAME)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(1) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            db.execSQL(
                                """
                                CREATE TABLE watchlist_items (
                                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                    media_type TEXT NOT NULL,
                                    tmdb_id INTEGER NOT NULL,
                                    title TEXT NOT NULL,
                                    poster_path TEXT,
                                    status TEXT NOT NULL,
                                    sort_order INTEGER NOT NULL,
                                    added_at INTEGER NOT NULL
                                )
                                """.trimIndent(),
                            )
                            db.execSQL(
                                "CREATE UNIQUE INDEX index_watchlist_items_tmdb_id_media_type " +
                                    "ON watchlist_items (tmdb_id, media_type)",
                            )
                        }

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) = Unit
                    },
                )
                .build(),
        )
        versionOneHelper.writableDatabase.apply {
            execSQL(
                """
                INSERT INTO watchlist_items (
                    media_type, tmdb_id, title, poster_path, status, sort_order, added_at
                ) VALUES ('MOVIE', 12345, 'Test Movie', '/poster.jpg', 'WATCHED', 0, 1000000)
                """.trimIndent(),
            )
        }
        versionOneHelper.close()

        var db = helper.runMigrationsAndValidate(
            TEST_DB_NAME,
            3,
            true,
            com.couchlist.app.core.data.local.MIGRATION_1_3,
        )

        db = helper.runMigrationsAndValidate(
            TEST_DB_NAME,
            4,
            true,
            com.couchlist.app.core.data.local.MIGRATION_3_4,
        )

        db = helper.runMigrationsAndValidate(
            TEST_DB_NAME,
            5,
            true,
            com.couchlist.app.core.data.local.MIGRATION_4_5,
        )

        db = helper.runMigrationsAndValidate(
            TEST_DB_NAME,
            6,
            true,
            com.couchlist.app.core.data.local.MIGRATION_5_6,
        )

        val cursor = db.query("SELECT source, category, external_id, title FROM media_items")
        assertTrue(cursor.moveToFirst())
        assertEquals("tmdb", cursor.getString(cursor.getColumnIndexOrThrow("source")))
        assertEquals("MOVIE", cursor.getString(cursor.getColumnIndexOrThrow("category")))
        assertEquals("12345", cursor.getString(cursor.getColumnIndexOrThrow("external_id")))
        cursor.close()

        db.query("SELECT COUNT(*) FROM lists WHERE type = 'PILE'").use { pileCursor ->
            assertTrue(pileCursor.moveToFirst())
            assertEquals(1, pileCursor.getInt(0))
        }
        db.query("SELECT mode, state FROM tracking_sessions").use { trackingCursor ->
            assertTrue(trackingCursor.moveToFirst())
            assertEquals("JUST_ENJOYING", trackingCursor.getString(0))
            assertEquals("COMPLETED", trackingCursor.getString(1))
        }
    }

    @Test
    fun migrate5To6BackfillsTrackingAndPreservesMemberships() {
        var db = helper.createDatabase(TEST_DB_NAME, 5).apply {
            insertV4Media(1, "MOVIE", "101", "Backlog movie")
            insertV4Media(2, "TV", "202", "Active show")
            insertV4Media(3, "BOOK", "303", "Paused book")
            insertV4Media(4, "MOVIE", "404", "Completed movie")
            insertV4Media(5, "VIDEO_GAME", "505", "Abandoned game")
            execSQL(
                """
                INSERT INTO library_items (
                    id, media_id, status, progress, personal_rating, favorite, notes,
                    added_at, started_at, completed_at, updated_at
                ) VALUES
                    (10, 1, 'BACKLOG', NULL, NULL, 0, NULL, 100, NULL, NULL, 110),
                    (11, 2, 'WATCHING', 0.25, 8, 1, 'note', 200, 210, NULL, 220),
                    (12, 3, 'BACKLOG', 0.5, NULL, 0, NULL, 300, NULL, NULL, 320),
                    (13, 4, 'COMPLETED', 1.0, NULL, 0, NULL, 400, 410, 450, 460),
                    (14, 5, 'ABANDONED', NULL, NULL, 0, NULL, 500, 510, NULL, 560)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO lists (
                    id, name, description, type, group_id, cover_media_id, is_pinned,
                    sort_order, smart_filter_json, created_at, updated_at
                ) VALUES (20, 'Queue', NULL, 'TODO', NULL, NULL, 0, 0, NULL, 100, 100)
                """.trimIndent(),
            )
            execSQL(
                "INSERT INTO media_list_joins (id, list_id, library_item_id, added_at) " +
                    "VALUES (30, 20, 11, 230)",
            )
            close()
        }

        db = helper.runMigrationsAndValidate(
            TEST_DB_NAME,
            6,
            true,
            com.couchlist.app.core.data.local.MIGRATION_5_6,
        )

        db.query("PRAGMA table_info(library_items)").use { cursor ->
            val columns = mutableSetOf<String>()
            while (cursor.moveToNext()) columns += cursor.getString(cursor.getColumnIndexOrThrow("name"))
            assertFalse("status" in columns)
            assertFalse("progress" in columns)
            assertFalse("started_at" in columns)
            assertFalse("completed_at" in columns)
            assertTrue("personal_rating" in columns)
        }
        db.query("SELECT COUNT(*) FROM tracking_sessions").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(4, cursor.getInt(0))
        }
        db.query(
            "SELECT mode, state, started_at, legacy_progress_fraction " +
                "FROM tracking_sessions WHERE library_item_id = 11",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("CHECKLIST", cursor.getString(0))
            assertEquals("ACTIVE", cursor.getString(1))
            assertEquals(210L, cursor.getLong(2))
            assertEquals(0.25, cursor.getDouble(3), 0.0)
        }
        db.query(
            "SELECT mode, state, legacy_progress_fraction " +
                "FROM tracking_sessions WHERE library_item_id = 12",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("SIMPLE_COUNTER", cursor.getString(0))
            assertEquals("PAUSED", cursor.getString(1))
            assertEquals(0.5, cursor.getDouble(2), 0.0)
        }
        db.query("SELECT state, ended_at FROM tracking_sessions WHERE library_item_id = 13").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("COMPLETED", cursor.getString(0))
            assertEquals(450L, cursor.getLong(1))
        }
        db.query("SELECT state, ended_at FROM tracking_sessions WHERE library_item_id = 14").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("ABANDONED", cursor.getString(0))
            assertEquals(560L, cursor.getLong(1))
        }
        db.query("SELECT library_item_id, added_at FROM media_list_joins WHERE id = 30").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(11L, cursor.getLong(0))
            assertEquals(230L, cursor.getLong(1))
        }
        db.query("PRAGMA foreign_key_check").use { cursor -> assertFalse(cursor.moveToFirst()) }
    }

    @Test
    fun migrate4To5PreservesListsAndBackfillsLibraryMemberships() {
        var db = helper.createDatabase(TEST_DB_NAME, 4).apply {
            insertV4Media(1, "MOVIE", "101", "First")
            insertV4Media(2, "TV", "202", "Second")
            execSQL(
                """
                INSERT INTO library_items (
                    id, media_id, status, progress, personal_rating, favorite, notes,
                    added_at, started_at, completed_at, updated_at
                ) VALUES (7, 2, 'BACKLOG', NULL, NULL, 0, NULL, 200, NULL, NULL, 200)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO lists (
                    id, name, description, type, cover_media_id, is_pinned, sort_order,
                    smart_filter_json, created_at, updated_at
                ) VALUES
                    (10, 'Favorites', 'Keepers', 'COLLECTION', 1, 1, 4, NULL, 100, 101),
                    (11, 'Inbox', NULL, 'PILE', NULL, 1, 0, NULL, 100, 100),
                    (12, 'Spare Pile', NULL, 'PILE', NULL, 1, 1, NULL, 100, 100)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO media_list_joins (id, list_id, media_id, added_at)
                VALUES (20, 10, 1, 300), (21, 12, 2, 400)
                """.trimIndent(),
            )
            close()
        }

        db = helper.runMigrationsAndValidate(
            TEST_DB_NAME,
            5,
            true,
            com.couchlist.app.core.data.local.MIGRATION_4_5,
        )

        db.query("SELECT name, description, cover_media_id, is_pinned, sort_order, group_id FROM lists WHERE id = 10").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Favorites", cursor.getString(0))
            assertEquals("Keepers", cursor.getString(1))
            assertEquals(1, cursor.getInt(2))
            assertEquals(1, cursor.getInt(3))
            assertEquals(4, cursor.getInt(4))
            assertTrue(cursor.isNull(5))
        }
        db.query("SELECT id, status FROM library_items WHERE media_id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            val libraryId = cursor.getLong(0)
            assertEquals("BACKLOG", cursor.getString(1))
            db.query("SELECT library_item_id, added_at FROM media_list_joins WHERE id = 20").use { join ->
                assertTrue(join.moveToFirst())
                assertEquals(libraryId, join.getLong(0))
                assertEquals(300L, join.getLong(1))
            }
        }
        db.query("SELECT COUNT(*) FROM lists WHERE type = 'PILE'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
        db.query("SELECT name, type FROM lists WHERE id = 12").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Spare Pile", cursor.getString(0))
            assertEquals("COLLECTION", cursor.getString(1))
        }
        db.query("SELECT name FROM lists WHERE id = 11").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("The Pile", cursor.getString(0))
        }
        db.query("PRAGMA foreign_key_check").use { cursor -> assertFalse(cursor.moveToFirst()) }
    }

    @Test
    fun migrate4To5CreatesMissingPile() {
        var db = helper.createDatabase(TEST_DB_NAME, 4).apply { close() }

        db = helper.runMigrationsAndValidate(
            TEST_DB_NAME,
            5,
            true,
            com.couchlist.app.core.data.local.MIGRATION_4_5,
        )

        db.query("SELECT name, is_pinned, group_id FROM lists WHERE type = 'PILE'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("The Pile", cursor.getString(0))
            assertEquals(1, cursor.getInt(1))
            assertTrue(cursor.isNull(2))
        }
    }

    private fun SupportSQLiteDatabase.insertV4Media(
        id: Long,
        category: String,
        externalId: String,
        title: String,
    ) {
        execSQL(
            """
            INSERT INTO media_items (
                id, source, category, external_id, title, original_title, description,
                artwork_uri, backdrop_uri, release_date, original_language, external_rating,
                external_vote_count, genres, last_refreshed_at, created_at, updated_at
            ) VALUES (?, 'tmdb', ?, ?, ?, NULL, NULL, NULL, NULL, NULL, NULL, 0.0, 0, NULL, NULL, 100, 100)
            """.trimIndent(),
            arrayOf<Any?>(id, category, externalId, title),
        )
    }

    private fun assertEquals(expected: Any?, actual: Any?) {
        org.junit.Assert.assertEquals(expected, actual)
    }

    private fun assertEquals(expected: Double, actual: Double, delta: Double) {
        org.junit.Assert.assertEquals(expected, actual, delta)
    }
}
