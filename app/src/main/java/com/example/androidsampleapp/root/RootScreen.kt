package com.example.androidsampleapp.root

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.androidsampleapp.core.mvi.CollectEffect
import com.example.androidsampleapp.navigation.AppNavHost
import com.example.androidsampleapp.navigation.TopLevelDestination

/**
 * アプリの外枠。Navigation bar のタップ -> Intent -> Reducer -> (State, Effect) を組み立てる場所。
 */
@Composable
fun RootScreen(
    viewModel: RootViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    // 遷移の実行は Effect を受けて行う。State を監視して navigate すると、
    // 画面回転などの再生成のたびに遷移が再実行されてしまう。
    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is RootEffect.NavigateToTab -> navController.navigateToTab(effect.destination)
            is RootEffect.PopToTabRoot -> navController.popBackStack(effect.destination.route, inclusive = false)
        }
    }

    // 戻るキーなどで現在地が変わったら State を追従させる（バーのハイライトのため）。
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentTab = backStackEntry?.destination?.let { destination ->
        TopLevelDestination.entries.firstOrNull { tab ->
            destination.hierarchy.any { it.route == tab.route }
        }
    }
    LaunchedEffect(currentTab) {
        if (currentTab != null && currentTab != state.selectedTab) {
            viewModel.dispatch(RootIntent.BackStackChanged(currentTab))
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = destination == state.selectedTab,
                        onClick = { viewModel.dispatch(RootIntent.TabClicked(destination)) },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = stringResource(destination.labelRes),
                            )
                        },
                        label = { Text(stringResource(destination.labelRes)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            snackbarHostState = snackbarHostState,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

/** タブ切り替えの定型。タブごとのバックスタックを保存・復元する。 */
private fun NavHostController.navigateToTab(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
