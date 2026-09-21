package com.couchlist.app.core.domain.repository

interface ExportImportRepository {
    suspend fun exportToJson(): String
    suspend fun importFromJson(jsonString: String)
}
