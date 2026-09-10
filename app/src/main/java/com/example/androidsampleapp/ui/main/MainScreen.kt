package com.example.androidsampleapp.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.androidsampleapp.core.mvi.CollectEffect
import com.example.androidsampleapp.domain.model.ConnectionState
import com.example.androidsampleapp.ui.aircon.AirconScreen
import com.example.androidsampleapp.ui.common.AppHeader
import com.example.androidsampleapp.ui.common.CenteredMessage
import com.example.androidsampleapp.ui.common.PanelPreview
import com.example.androidsampleapp.ui.common.PreviewSurface
import com.example.androidsampleapp.ui.common.Route
import com.example.androidsampleapp.ui.top.TopScreen

/**
 * 下部バーを持つメイン画面。タブの中身は内側の NavHost が持ち、
 * スリープ画面だけは外側（AppNavigation）のルートへ抜ける。
 *
 * NavController まわりの配線はここに置き、枠の描画は [MainContent] に分けてある。
 */
@Composable
fun MainScreen(
    onNavigateToSleep: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is MainEffect.NavigateToTab -> navController.navigateToTab(effect.tab)
            is MainEffect.PopToTabRoot -> navController.popBackStack(effect.tab.route, inclusive = false)
            MainEffect.NavigateToSleep -> onNavigateToSleep()
        }
    }

    // 戻る操作で現在地が変わったら State を追従させる（バーのハイライトのため）。
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentTab = backStackEntry?.destination?.let { destination ->
        MainTab.entries.firstOrNull { tab -> destination.hierarchy.any { it.route == tab.route } }
    }
    LaunchedEffect(currentTab) {
        if (currentTab != null && currentTab != state.selectedTab) {
            viewModel.dispatch(MainIntent.BackStackChanged(currentTab))
        }
    }

    MainContent(
        state = state,
        onIntent = viewModel::dispatch,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    ) {
        NavHost(
            navController = navController,
            startDestination = Route.TOP,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(Route.TOP) { TopScreen(snackbarHostState = snackbarHostState) }
            composable(Route.AIRCON) { AirconScreen(snackbarHostState = snackbarHostState) }
        }
    }
}

@Composable
private fun MainContent(
    state: MainState,
    onIntent: (MainIntent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            BottomNaviBar(
                selectedTab = state.selectedTab,
                onTabClick = { onIntent(MainIntent.TabClicked(it)) },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            AppHeader(
                title = stringResource(state.selectedTab.labelRes),
                connectionState = state.connectionState,
            )
            content()
        }
    }
}

/** タブ切り替えの定型。タブごとのバックスタックを保存・復元する。 */
private fun NavHostController.navigateToTab(tab: MainTab) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@PanelPreview
@Composable
private fun MainContentConnectedPreview() {
    PreviewSurface {
        MainContent(
            state = MainState(
                selectedTab = MainTab.TOP,
                connectionState = ConnectionState.CONNECTED,
            ),
            onIntent = {},
            snackbarHostState = remember { SnackbarHostState() },
        ) {
            CenteredMessage(message = "タブの中身")
        }
    }
}

@PanelPreview
@Composable
private fun MainContentDisconnectedPreview() {
    PreviewSurface {
        MainContent(
            state = MainState(
                selectedTab = MainTab.AIRCON,
                connectionState = ConnectionState.DISCONNECTED,
            ),
            onIntent = {},
            snackbarHostState = remember { SnackbarHostState() },
        ) {
            CenteredMessage(message = "タブの中身")
        }
    }
}
