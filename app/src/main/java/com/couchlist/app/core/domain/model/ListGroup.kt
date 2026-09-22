package com.couchlist.app.core.domain.model

data class ListGroup(
    val id: Long,
    val name: String,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
)
