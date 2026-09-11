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
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.androidsampleapp.ui.aircon.AirconRoute
import com.example.androidsampleapp.ui.common.Route
import com.example.androidsampleapp.ui.contact.ContactRoute
import com.example.androidsampleapp.ui.main.MainRoute
import com.example.androidsampleapp.ui.main.MainTab
import com.example.androidsampleapp.ui.main.toRoute
import com.example.androidsampleapp.ui.sleep.SleepRoute
import com.example.androidsampleapp.ui.top.TopRoute

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
    missedCall: MissedCallViewModel = hiltViewModel(),
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

    // 不在着信は HTTP なので黙っていても届かない。画面が切り替わる節目で取りに行く。
    // 起動直後に 1 度取らないと、最初のタブ移動まで下部バーのバッジが出ない。
    LaunchedEffect(Unit) {
        missedCall.refresh()
    }

    LaunchedEffect(isSleeping) {
        if (isSleeping) {
            // スリープ画面が前に出たタイミング。
            missedCall.refresh()
            if (navController.currentDestination?.route != Route.SLEEP) {
                navController.navigate(Route.SLEEP) { launchSingleTop = true }
            }
        } else {
            // 直前に見ていたタブへ戻る。スタックに無ければ何も起きない。
            navController.popBackStack(Route.SLEEP, inclusive = true)
        }
    }

    val onTabClick: (MainTab) -> Unit = { tab ->
        missedCall.refresh()
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
                MainRoute(
                    // 行き先はルートで指定し、選択するタブはそこから引く。
                    selectedTab = MainTab.fromRoute(Route.TOP),
                    onTabClick = onTabClick,
                    snackbarHostState = snackbarHostState,
                ) {
                    TopRoute(snackbarHostState = snackbarHostState)
                }
            }

            composable(Route.AIRCON) {
                MainRoute(
                    selectedTab = MainTab.fromRoute(Route.AIRCON),
                    onTabClick = onTabClick,
                    snackbarHostState = snackbarHostState,
                ) {
                    AirconRoute(snackbarHostState = snackbarHostState)
                }
            }

            composable(
                route = Route.CONTACT_PATTERN,
                arguments = listOf(
                    navArgument(Route.ARG_CONTACT_LIST) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) {
                MainRoute(
                    selectedTab = MainTab.fromRoute(Route.CONTACT),
                    onTabClick = onTabClick,
                    snackbarHostState = snackbarHostState,
                ) {
                    ContactRoute()
                }
            }

            composable(Route.SLEEP) {
                SleepRoute(
                    onUnlock = { idleTimer.wake() },
                    onNoticeSelected = { destination ->
                        // 引数つきで開きたいので、タブではなくルートを組み立てて渡す。
                        // 状態を復元すると前回の引数のまま開いてしまうため、復元はしない。
                        navController.navigateToRoute(destination.toRoute(), restore = false)
                        idleTimer.wake()
                    },
                )
            }
        }
    }
}

/** タブ切り替えの定型。タブごとのバックスタックを保存・復元する。 */
private fun NavHostController.navigateToTab(tab: MainTab) = navigateToRoute(tab.route)

/**
 * 遷移の定型。タブごとのバックスタックを保存し、既定では復元して戻る。
 *
 * 引数つきのルートへ飛ぶときは [restore] を false にする。復元すると保存済みの
 * エントリがそのまま戻り、新しく渡した引数が無視されるため。
 */
private fun NavHostController.navigateToRoute(route: String, restore: Boolean = true) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = restore
    }
}
