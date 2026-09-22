package com.couchlist.app.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded

data class MediaListSummaryRow(
    @Embedded
    val list: MediaListEntity,
    @ColumnInfo(name = "item_count")
    val itemCount: Int,
    @ColumnInfo(name = "cover_artwork_uri")
    val coverArtworkUri: String?,
)
