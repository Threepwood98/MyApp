package com.couchlist.app.core.data.local.entity

import androidx.room.ColumnInfo
import com.couchlist.app.core.domain.model.MediaCategory

data class MediaReferenceRow(
    @ColumnInfo(name = "source")
    val source: String,
    @ColumnInfo(name = "category")
    val category: MediaCategory,
    @ColumnInfo(name = "external_id")
    val externalId: String,
)
