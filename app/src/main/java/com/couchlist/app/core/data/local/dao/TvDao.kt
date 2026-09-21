package com.couchlist.app.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.couchlist.app.core.data.local.entity.EpisodeEntity
import com.couchlist.app.core.data.local.entity.LogEntryEntity
import com.couchlist.app.core.data.local.entity.SeasonEntity
import com.couchlist.app.core.data.local.entity.TvEpisodeWithStateRow
import com.couchlist.app.core.data.local.entity.TvProgressRow
import com.couchlist.app.core.domain.model.LogAction
import kotlinx.coroutines.flow.Flow

@Dao
interface TvDao {

    @Query("SELECT * FROM seasons WHERE media_id = :mediaId ORDER BY season_number")
    fun observeSeasons(mediaId: Long): Flow<List<SeasonEntity>>

    @Query(
        "SELECT episodes.*, " +
            "CASE WHEN COALESCE((" +
            "SELECT action FROM log_entries " +
            "WHERE episode_id = episodes.id " +
            "AND action IN ('EPISODE_WATCHED', 'EPISODE_UNWATCHED') " +
            "ORDER BY id DESC LIMIT 1" +
            "), 'EPISODE_UNWATCHED') = 'EPISODE_WATCHED' THEN 1 ELSE 0 END AS is_watched " +
            "FROM episodes " +
            "WHERE media_id = :mediaId AND season_number = :seasonNumber " +
            "ORDER BY episode_number",
    )
    fun observeEpisodes(
        mediaId: Long,
        seasonNumber: Int,
    ): Flow<List<TvEpisodeWithStateRow>>

    @Query(
        "SELECT " +
            "(SELECT COUNT(*) FROM episodes AS episode " +
            "WHERE episode.media_id = :mediaId AND episode.season_number > 0 " +
            "AND COALESCE((SELECT action FROM log_entries " +
            "WHERE episode_id = episode.id " +
            "AND action IN ('EPISODE_WATCHED', 'EPISODE_UNWATCHED') " +
            "ORDER BY id DESC LIMIT 1), 'EPISODE_UNWATCHED') = 'EPISODE_WATCHED') AS watched_episodes, " +
            "COALESCE((SELECT SUM(episode_count) FROM seasons " +
            "WHERE media_id = :mediaId AND season_number > 0), 0) AS total_episodes " +
            "FROM media_items WHERE id = :mediaId",
    )
    fun observeProgress(mediaId: Long): Flow<TvProgressRow?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSeasonIgnore(season: SeasonEntity): Long

    @Query(
        "UPDATE seasons SET name = :name, overview = :overview, artwork_uri = :artworkUri, " +
            "air_date = :airDate, episode_count = :episodeCount, " +
            "last_refreshed_at = :lastRefreshedAt " +
            "WHERE media_id = :mediaId AND season_number = :seasonNumber",
    )
    suspend fun updateSeasonMetadata(
        mediaId: Long,
        seasonNumber: Int,
        name: String,
        overview: String?,
        artworkUri: String?,
        airDate: String?,
        episodeCount: Int,
        lastRefreshedAt: Long?,
    )

    @Query("SELECT id FROM seasons WHERE media_id = :mediaId AND season_number = :seasonNumber")
    suspend fun getSeasonId(mediaId: Long, seasonNumber: Int): Long?

    @Transaction
    suspend fun upsertSeason(season: SeasonEntity): Long {
        val insertedId = insertSeasonIgnore(season)
        if (insertedId != -1L) return insertedId
        updateSeasonMetadata(
            mediaId = season.mediaId,
            seasonNumber = season.seasonNumber,
            name = season.name,
            overview = season.overview,
            artworkUri = season.artworkUri,
            airDate = season.airDate,
            episodeCount = season.episodeCount,
            lastRefreshedAt = season.lastRefreshedAt,
        )
        return checkNotNull(getSeasonId(season.mediaId, season.seasonNumber))
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEpisodeIgnore(episode: EpisodeEntity): Long

    @Query(
        "UPDATE episodes SET title = :title, overview = :overview, artwork_uri = :artworkUri, " +
            "air_date = :airDate, runtime_minutes = :runtimeMinutes " +
            "WHERE season_id = :seasonId AND episode_number = :episodeNumber",
    )
    suspend fun updateEpisodeMetadata(
        seasonId: Long,
        episodeNumber: Int,
        title: String,
        overview: String?,
        artworkUri: String?,
        airDate: String?,
        runtimeMinutes: Int?,
    )

    @Transaction
    suspend fun upsertEpisode(episode: EpisodeEntity) {
        if (insertEpisodeIgnore(episode) != -1L) return
        updateEpisodeMetadata(
            seasonId = episode.seasonId,
            episodeNumber = episode.episodeNumber,
            title = episode.title,
            overview = episode.overview,
            artworkUri = episode.artworkUri,
            airDate = episode.airDate,
            runtimeMinutes = episode.runtimeMinutes,
        )
    }

    @Query("SELECT * FROM episodes WHERE id = :episodeId")
    suspend fun getEpisode(episodeId: Long): EpisodeEntity?

    @Query(
        "SELECT action FROM log_entries WHERE episode_id = :episodeId " +
            "AND action IN ('EPISODE_WATCHED', 'EPISODE_UNWATCHED') " +
            "ORDER BY id DESC LIMIT 1",
    )
    suspend fun getLatestEpisodeAction(episodeId: Long): LogAction?

    @Insert
    suspend fun insertLogEntry(entry: LogEntryEntity): Long

    @Query(
        "SELECT COUNT(*) FROM episodes AS episode " +
            "WHERE episode.media_id = :mediaId AND episode.season_number > 0 " +
            "AND COALESCE((SELECT action FROM log_entries " +
            "WHERE episode_id = episode.id " +
            "AND action IN ('EPISODE_WATCHED', 'EPISODE_UNWATCHED') " +
            "ORDER BY id DESC LIMIT 1), 'EPISODE_UNWATCHED') = 'EPISODE_WATCHED'",
    )
    suspend fun getWatchedEpisodeCount(mediaId: Long): Int

    @Query(
        "SELECT COALESCE(SUM(episode_count), 0) FROM seasons " +
            "WHERE media_id = :mediaId AND season_number > 0",
    )
    suspend fun getTotalEpisodeCount(mediaId: Long): Int

    @Query(
        "SELECT episodes.*, " +
            "CASE WHEN (" +
            "SELECT action FROM log_entries " +
            "WHERE episode_id = episodes.id " +
            "AND action IN ('EPISODE_WATCHED', 'EPISODE_UNWATCHED') " +
            "ORDER BY id DESC LIMIT 1" +
            ") = 'EPISODE_WATCHED' THEN 1 ELSE 0 END AS is_watched " +
            "FROM episodes " +
            "WHERE episodes.media_id = :mediaId AND episodes.season_number > 0 " +
            "AND COALESCE((" +
            "SELECT action FROM log_entries " +
            "WHERE episode_id = episodes.id " +
            "AND action IN ('EPISODE_WATCHED', 'EPISODE_UNWATCHED') " +
            "ORDER BY id DESC LIMIT 1" +
            "), 'EPISODE_UNWATCHED') != 'EPISODE_WATCHED' " +
            "ORDER BY episodes.season_number, episodes.episode_number " +
            "LIMIT 1",
    )
    fun observeNextUnwatched(mediaId: Long): Flow<TvEpisodeWithStateRow?>
}
