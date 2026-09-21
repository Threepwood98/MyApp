package com.couchlist.app.core.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.couchlist.app.feature.detail.DetailRoute as DetailScreen
import com.couchlist.app.feature.home.HomeRoute
import com.couchlist.app.feature.library.LibraryRoute as LibraryScreen
import com.couchlist.app.feature.logbook.LogbookRoute as LogbookScreen
import com.couchlist.app.feature.search.SearchRoute as SearchScreen
import com.couchlist.app.feature.settings.SettingsRoute as SettingsScreen
import com.couchlist.app.feature.statistics.StatisticsRoute as StatisticsScreen

@Composable
fun CouchlistNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val destinations = listOf(
        TopLevelDestination(HomeRoute, "Home", Icons.Filled.Home),
        TopLevelDestination(SearchRoute, "Search", Icons.Filled.Search),
        TopLevelDestination(LibraryRoute, "Library", Icons.AutoMirrored.Filled.List),
        TopLevelDestination(LogbookRoute, "Logbook", Icons.Filled.DateRange),
        TopLevelDestination(StatisticsRoute, "Stats", Icons.Filled.Info),
    )
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = currentDestination == null || destinations.any {
        currentDestination.matches(it.route)
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(tonalElevation = 0.dp) {
                    destinations.forEach { destination ->
                        val selected = currentDestination.matches(destination.route)
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = destination.label,
                                )
                            },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<HomeRoute> {
                HomeRoute(
                    onSearchClick = { navController.navigateTopLevel(SearchRoute) },
                    onSettingsClick = { navController.navigate(SettingsRoute) },
                    onItemClick = { item -> navController.navigate(DetailRoute(item.media.id)) },
                )
            }
            composable<SearchRoute> {
                SearchScreen(
                    showBack = false,
                    onDetailClick = { mediaId -> navController.navigate(DetailRoute(mediaId)) },
                )
            }
            composable<LibraryRoute> {
                LibraryScreen(
                    onItemClick = { item -> navController.navigate(DetailRoute(item.media.id)) },
                )
            }
            composable<LogbookRoute> {
                LogbookScreen()
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
        }
    }
}

private fun NavHostController.navigateTopLevel(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavDestination?.matches(route: Any): Boolean = when (route) {
    HomeRoute -> this?.hasRoute<HomeRoute>() == true
    SearchRoute -> this?.hasRoute<SearchRoute>() == true
    LibraryRoute -> this?.hasRoute<LibraryRoute>() == true
    LogbookRoute -> this?.hasRoute<LogbookRoute>() == true
    StatisticsRoute -> this?.hasRoute<StatisticsRoute>() == true
    else -> false
}

private data class TopLevelDestination(
    val route: Any,
    val label: String,
    val icon: ImageVector,
)
