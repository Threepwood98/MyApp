package com.couchlist.app.core.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.couchlist.app.feature.detail.DetailRoute as DetailScreen
import com.couchlist.app.feature.home.HomeRoute
import com.couchlist.app.feature.search.SearchRoute
import com.couchlist.app.feature.settings.SettingsRoute

@Composable
fun CouchlistNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier,
    ) {
        composable<HomeRoute> {
            HomeRoute(
                onSearchClick = { navController.navigate(SearchRoute) },
                onSettingsClick = { navController.navigate(SettingsRoute) },
                onItemClick = { item ->
                    navController.navigate(DetailRoute(item.tmdbId, item.mediaType))
                },
            )
        }
        composable<SettingsRoute> {
            SettingsRoute(onBack = { navController.popBackStack() })
        }
        composable<SearchRoute> {
            SearchRoute(
                onBack = { navController.popBackStack() },
                onResultClick = { result ->
                    navController.navigate(DetailRoute(result.id, result.mediaType))
                },
            )
        }
        composable<DetailRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<DetailRoute>()
            DetailScreen(
                tmdbId = route.tmdbId,
                mediaType = route.mediaType,
                onBack = { navController.popBackStack() },
            )
        }
    }
}