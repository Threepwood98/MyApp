package com.couchlist.app.core.data.local.entity

import androidx.room.ColumnInfo

data class RatingCount(
    @ColumnInfo(name = "personal_rating") val rating: Int,
    val count: Int,
)

data class MonthCount(
    val month: String,
    val count: Int,
)
