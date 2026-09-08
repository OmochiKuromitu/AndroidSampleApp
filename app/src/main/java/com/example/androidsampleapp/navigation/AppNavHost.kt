package com.example.androidsampleapp.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.example.androidsampleapp.feature.home.HomeScreen
import com.example.androidsampleapp.feature.home.detail.DetailScreen
import com.example.androidsampleapp.feature.profile.ProfileScreen
import com.example.androidsampleapp.feature.search.SearchScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = TopLevelDestination.HOME.route,
        modifier = modifier,
    ) {
        // ホームタブはネストグラフ。タブ内の遷移（一覧 -> 詳細）はここに閉じる。
        navigation(
            route = TopLevelDestination.HOME.route,
            startDestination = HomeRoutes.LIST,
        ) {
            composable(HomeRoutes.LIST) {
                HomeScreen(
                    onOpenDetail = { taskId -> navController.navigate(HomeRoutes.detail(taskId)) },
                )
            }
            composable(
                route = HomeRoutes.DETAIL,
                arguments = listOf(navArgument(HomeRoutes.ARG_TASK_ID) { type = NavType.StringType }),
            ) {
                DetailScreen(
                    onBack = { navController.popBackStack() },
                    snackbarHostState = snackbarHostState,
                )
            }
        }

        composable(TopLevelDestination.SEARCH.route) {
            SearchScreen()
        }

        composable(TopLevelDestination.PROFILE.route) {
            ProfileScreen(snackbarHostState = snackbarHostState)
        }
    }
}
