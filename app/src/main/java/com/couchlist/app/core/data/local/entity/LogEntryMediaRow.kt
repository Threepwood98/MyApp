package com.couchlist.app.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded

data class LogEntryMediaRow(
    @Embedded
    val logEntry: LogEntryEntity,
    @ColumnInfo(name = "m_title")
    val mediaTitle: String,
    @ColumnInfo(name = "m_poster_path")
    val posterPath: String?,
    @ColumnInfo(name = "m_media_type")
    val mediaType: String,
)
