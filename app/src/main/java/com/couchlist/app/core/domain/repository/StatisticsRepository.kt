package com.couchlist.app.core.domain.repository

import com.couchlist.app.core.domain.model.Statistics
import kotlinx.coroutines.flow.Flow

interface StatisticsRepository {
    fun observeStatistics(): Flow<Statistics>
}
