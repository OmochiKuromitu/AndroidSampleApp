package com.example.androidsampleapp.ui.main

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * ヘッダーと下部バーの枠の配線。ここが AppNavigation から呼ばれる入口になる。
 *
 * NavController は持たない。どのタブを表示しているかは AppNavigation が決めて
 * [selectedTab] で渡し、タップは [onTabClick] で返すだけ。
 */
@Composable
fun MainRoute(
    selectedTab: MainTab,
    onTabClick: (MainTab) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    MainScreen(
        state = state,
        selectedTab = selectedTab,
        onTabClick = onTabClick,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
        content = content,
    )
}
