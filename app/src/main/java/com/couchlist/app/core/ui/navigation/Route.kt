package com.couchlist.app.core.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object ListsRoute

@Serializable
data object SettingsRoute

@Serializable
data object SearchRoute

@Serializable
data object LibraryRoute

@Serializable
data object LogbookRoute

@Serializable
data object StatisticsRoute

@Serializable
data class DetailRoute(
    val mediaId: Long,
)

@Serializable
data class ListDetailRoute(
    val listId: Long,
)
