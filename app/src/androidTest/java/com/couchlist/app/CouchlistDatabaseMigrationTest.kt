package com.couchlist.app

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import com.couchlist.app.core.data.local.CouchlistDatabase
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
        assert(cursor.moveToFirst())
        assertEquals("tmdb", cursor.getString(cursor.getColumnIndexOrThrow("source")))
        assertEquals("MOVIE", cursor.getString(cursor.getColumnIndexOrThrow("category")))
        assertEquals("12345", cursor.getString(cursor.getColumnIndexOrThrow("external_id")))
        assertEquals("Test Movie", cursor.getString(cursor.getColumnIndexOrThrow("title")))
        assertEquals("A overview", cursor.getString(cursor.getColumnIndexOrThrow("description")))
        assertEquals("https://image.tmdb.org/t/p/w500/poster.jpg", cursor.getString(cursor.getColumnIndexOrThrow("artwork_uri")))
        assertEquals("https://image.tmdb.org/t/p/w780/backdrop.jpg", cursor.getString(cursor.getColumnIndexOrThrow("backdrop_uri")))
        cursor.close()

        val runtimeCursor = db.query("SELECT runtime_minutes FROM video_metadata WHERE media_id = 1")
        assert(runtimeCursor.moveToFirst())
        assertEquals(120, runtimeCursor.getInt(runtimeCursor.getColumnIndexOrThrow("runtime_minutes")))
        runtimeCursor.close()

        val seasonCursor = db.query("SELECT artwork_uri, name FROM seasons WHERE media_id = 2")
        assert(seasonCursor.moveToFirst())
        assertEquals("Season 1", seasonCursor.getString(seasonCursor.getColumnIndexOrThrow("name")))
        assertEquals("https://image.tmdb.org/t/p/w500/season.jpg", seasonCursor.getString(seasonCursor.getColumnIndexOrThrow("artwork_uri")))
        seasonCursor.close()

        val episodeCursor = db.query("SELECT artwork_uri, runtime_minutes FROM episodes WHERE media_id = 2")
        assert(episodeCursor.moveToFirst())
        assertEquals("https://image.tmdb.org/t/p/w300/still.jpg", episodeCursor.getString(episodeCursor.getColumnIndexOrThrow("artwork_uri")))
        assertEquals(45, episodeCursor.getInt(episodeCursor.getColumnIndexOrThrow("runtime_minutes")))
        episodeCursor.close()

        val libCursor = db.query("SELECT media_id, status FROM library_items WHERE media_id = 1")
        assert(libCursor.moveToFirst())
        assertEquals(1, libCursor.getInt(libCursor.getColumnIndexOrThrow("media_id")))
        assertEquals("COMPLETED", libCursor.getString(libCursor.getColumnIndexOrThrow("status")))
        libCursor.close()

        val logCursor = db.query("SELECT action FROM log_entries WHERE media_id = 1 AND action = 'COMPLETED'")
        assert(logCursor.moveToFirst())
        assertEquals("COMPLETED", logCursor.getString(logCursor.getColumnIndexOrThrow("action")))
        logCursor.close()
    }

    @Test
    fun migrate1To3To4() {
        var db = helper.createDatabase(TEST_DB_NAME, 1).apply {
            execSQL(
                """
                INSERT INTO watchlist_items (tmdb_id, media_type, status, added_at)
                VALUES (12345, 'MOVIE', 'WATCHED', 1000000)
                """.trimIndent(),
            )
            close()
        }

        db = helper.runMigrationsAndValidate(
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

        val cursor = db.query("SELECT source, category, external_id, title FROM media_items")
        assert(cursor.moveToFirst())
        assertEquals("tmdb", cursor.getString(cursor.getColumnIndexOrThrow("source")))
        assertEquals("MOVIE", cursor.getString(cursor.getColumnIndexOrThrow("category")))
        assertEquals("12345", cursor.getString(cursor.getColumnIndexOrThrow("external_id")))
        cursor.close()
    }

    private fun assertEquals(expected: Any?, actual: Any?) {
        org.junit.Assert.assertEquals(expected, actual)
    }
}
