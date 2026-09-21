package com.couchlist.app.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded

data class LogEntryMediaRow(
    @Embedded
    val logEntry: LogEntryEntity,
    @ColumnInfo(name = "m_title")
    val mediaTitle: String,
    @ColumnInfo(name = "m_artwork_uri")
    val artworkUri: String?,
    @ColumnInfo(name = "m_category")
    val category: String,
)
