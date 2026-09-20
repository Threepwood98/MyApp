package com.couchlist.app.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded

data class TvEpisodeWithStateRow(
    @Embedded
    val episode: EpisodeEntity,
    @ColumnInfo(name = "is_watched")
    val isWatched: Boolean,
)

data class TvProgressRow(
    @ColumnInfo(name = "watched_episodes")
    val watchedEpisodes: Int,
    @ColumnInfo(name = "total_episodes")
    val totalEpisodes: Int,
)
