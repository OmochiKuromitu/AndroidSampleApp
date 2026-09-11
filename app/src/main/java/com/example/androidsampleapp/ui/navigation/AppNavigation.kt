package com.example.androidsampleapp.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.SnackbarHostState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.androidsampleapp.ui.aircon.AirconScreen
import com.example.androidsampleapp.ui.common.Route
import com.example.androidsampleapp.ui.main.MainScreen
import com.example.androidsampleapp.ui.main.MainTab
import com.example.androidsampleapp.ui.main.toMainTab
import com.example.androidsampleapp.ui.sleep.SleepScreen
import com.example.androidsampleapp.ui.top.TopScreen

/**
 * アプリの遷移をすべてここで行う。
 *
 * 各画面の ViewModel は「何が起きたか」を返すだけで、どこへ行くかは決めない。
 * NavController もここだけが持つ。遷移の判断と実行が 1 か所に揃っていないと、
 * 「この画面はどこから来てどこへ行くのか」を追うのにファイルを行き来する羽目になる。
 *
 * NavHost は 1 つ。スリープ画面だけ枠（ヘッダーと下部バー）を被せずに出す。
 */
@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    idleTimer: IdleTimerViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val isSleeping by idleTimer.isSleeping.collectAsStateWithLifecycle()

    // 着信したら起こしてトップを出す。検知は IncomingCallRouter、行き先はここ。
    IncomingCallRouter(
        onIncomingCall = {
            navController.navigateToTab(MainTab.TOP)
            idleTimer.wake()
        },
    )

    LaunchedEffect(isSleeping) {
        if (isSleeping) {
            if (navController.currentDestination?.route != Route.SLEEP) {
                navController.navigate(Route.SLEEP) { launchSingleTop = true }
            }
        } else {
            // 直前に見ていたタブへ戻る。スタックに無ければ何も起きない。
            navController.popBackStack(Route.SLEEP, inclusive = true)
        }
    }

    val onTabClick: (MainTab) -> Unit = { tab ->
        // スリープは画面ではなく状態。遷移は isSleeping を見た上の LaunchedEffect が行う。
        if (tab == MainTab.SLEEP) idleTimer.onSleepRequested() else navController.navigateToTab(tab)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            // 子より先にイベントを覗いて無操作タイマーを戻す。子の操作は妨げない。
            // スリープ中は IdleTimer 側で無視されるので、触れただけでは解除されない。
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial)
                        idleTimer.resetTimer()
                    }
                }
            },
    ) {
        NavHost(navController = navController, startDestination = Route.TOP) {
            composable(Route.TOP) {
                MainScreen(
                    selectedTab = MainTab.TOP,
                    onTabClick = onTabClick,
                    snackbarHostState = snackbarHostState,
                ) {
                    TopScreen(snackbarHostState = snackbarHostState)
                }
            }

            composable(Route.AIRCON) {
                MainScreen(
                    selectedTab = MainTab.AIRCON,
                    onTabClick = onTabClick,
                    snackbarHostState = snackbarHostState,
                ) {
                    AirconScreen(snackbarHostState = snackbarHostState)
                }
            }

            composable(Route.SLEEP) {
                SleepScreen(
                    onUnlock = { idleTimer.wake() },
                    onNoticeSelected = { destination ->
                        navController.navigateToTab(destination.toMainTab())
                        idleTimer.wake()
                    },
                )
            }
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
