package com.example.androidsampleapp.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.androidsampleapp.domain.model.ConnectionState
import com.example.androidsampleapp.ui.common.AppHeader
import com.example.androidsampleapp.ui.common.CenteredMessage
import com.example.androidsampleapp.ui.common.PanelPreview
import com.example.androidsampleapp.ui.common.PreviewSurface

/**
 * ヘッダーと下部バーの枠。中身は [content] として受け取る。
 * 表示だけを担当し、ViewModel は知らない。
 *
 * 入口は [MainRoute]。
 */
@Composable
fun MainScreen(
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
private fun MainScreenConnectedPreview() {
    PreviewSurface {
        MainScreen(
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
private fun MainScreenDisconnectedPreview() {
    PreviewSurface {
        MainScreen(
            state = MainState(connectionState = ConnectionState.DISCONNECTED),
            selectedTab = MainTab.AIRCON,
            onTabClick = {},
            snackbarHostState = remember { SnackbarHostState() },
        ) {
            CenteredMessage(message = "タブの中身")
        }
    }
}
