package com.couchlist.app.core.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.couchlist.app.R
import com.couchlist.app.feature.detail.DetailRoute as DetailScreen
import com.couchlist.app.feature.library.LibraryRoute as LibraryScreen
import com.couchlist.app.feature.lists.ListDetailRoute as ListDetailScreen
import com.couchlist.app.feature.lists.ListsRoute as ListsScreen
import com.couchlist.app.feature.logbook.LogbookRoute as LogbookScreen
import com.couchlist.app.feature.search.SearchRoute as SearchScreen
import com.couchlist.app.feature.settings.SettingsRoute as SettingsScreen
import com.couchlist.app.feature.statistics.StatisticsRoute as StatisticsScreen

@Composable
fun CouchlistNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = currentDestination == null || TopLevelDestination.entries.any {
        it.matches(currentDestination)
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    TopLevelDestination.entries.forEach { destination ->
                        val label = stringResource(destination.labelRes)
                        NavigationBarItem(
                            selected = destination.matches(currentDestination),
                            onClick = { destination.navigate(navController) },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = null,
                                )
                            },
                            label = { Text(label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = ListsRoute,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            composable<ListsRoute> {
                ListsScreen(
                    onListClick = { listId -> navController.navigate(ListDetailRoute(listId)) },
                    onItemClick = { mediaId -> navController.navigate(DetailRoute(mediaId)) },
                    onSearchClick = { TopLevelDestination.SEARCH.navigate(navController) },
                    onLibraryClick = { navController.navigate(LibraryRoute) },
                    onStatisticsClick = { navController.navigate(StatisticsRoute) },
                    onSettingsClick = { navController.navigate(SettingsRoute) },
                )
            }
            composable<SearchRoute> {
                SearchScreen(
                    showBack = false,
                    onDetailClick = { mediaId -> navController.navigate(DetailRoute(mediaId)) },
                )
            }
            composable<LogbookRoute> {
                LogbookScreen()
            }
            composable<LibraryRoute> {
                LibraryScreen(
                    onItemClick = { item -> navController.navigate(DetailRoute(item.media.id)) },
                )
            }
            composable<StatisticsRoute> {
                StatisticsScreen()
            }
            composable<SettingsRoute> {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
            composable<DetailRoute> {
                DetailScreen(onBack = { navController.popBackStack() })
            }
            composable<ListDetailRoute> {
                ListDetailScreen(
                    onBack = { navController.popBackStack() },
                    onItemClick = { mediaId -> navController.navigate(DetailRoute(mediaId)) },
                )
            }
        }
    }
}

private enum class TopLevelDestination(
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    LISTS(R.string.navigation_lists, Icons.AutoMirrored.Filled.List),
    SEARCH(R.string.navigation_search, Icons.Filled.Search),
    LOGBOOK(R.string.navigation_logbook, Icons.Filled.DateRange),
    ;

    fun matches(destination: NavDestination?): Boolean = when (this) {
        LISTS -> destination?.hasRoute<ListsRoute>() == true ||
            destination?.hasRoute<LibraryRoute>() == true ||
            destination?.hasRoute<StatisticsRoute>() == true
        SEARCH -> destination?.hasRoute<SearchRoute>() == true
        LOGBOOK -> destination?.hasRoute<LogbookRoute>() == true
    }

    fun navigate(navController: NavHostController) {
        val route = when (this) {
            LISTS -> ListsRoute
            SEARCH -> SearchRoute
            LOGBOOK -> LogbookRoute
        }
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
}
