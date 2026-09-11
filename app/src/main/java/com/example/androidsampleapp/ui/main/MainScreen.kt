package com.example.androidsampleapp.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidsampleapp.domain.model.ConnectionState
import com.example.androidsampleapp.ui.common.AppHeader
import com.example.androidsampleapp.ui.common.CenteredMessage
import com.example.androidsampleapp.ui.common.PanelPreview
import com.example.androidsampleapp.ui.common.PreviewSurface

/**
 * ヘッダーと下部バーを持つ枠。中身は [content] として受け取る。
 *
 * NavController は持たない。どのタブを表示しているかは AppNavigation が決めて
 * [selectedTab] で渡し、タップは [onTabClick] で返すだけ。
 * 遷移の判断と実行を 1 か所に寄せるため、この画面は見た目だけを担当する。
 */
@Composable
fun MainScreen(
    selectedTab: MainTab,
    onTabClick: (MainTab) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    MainContent(
        state = state,
        selectedTab = selectedTab,
        onTabClick = onTabClick,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
        content = content,
    )
}

@Composable
private fun MainContent(
    state: MainState,
    selectedTab: MainTab,
    onTabClick: (MainTab) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            BottomNaviBar(selectedTab = selectedTab, onTabClick = onTabClick)
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            AppHeader(
                title = stringResource(selectedTab.labelRes),
                connectionState = state.connectionState,
            )
            content()
        }
    }
}

@PanelPreview
@Composable
private fun MainContentConnectedPreview() {
    PreviewSurface {
        MainContent(
            state = MainState(connectionState = ConnectionState.CONNECTED),
            selectedTab = MainTab.TOP,
            onTabClick = {},
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
            state = MainState(connectionState = ConnectionState.DISCONNECTED),
            selectedTab = MainTab.AIRCON,
            onTabClick = {},
            snackbarHostState = remember { SnackbarHostState() },
        ) {
            CenteredMessage(message = "タブの中身")
        }
    }
}
