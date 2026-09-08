package com.example.androidsampleapp.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.androidsampleapp.core.AppStateHolder
import com.example.androidsampleapp.ui.common.Route
import com.example.androidsampleapp.ui.main.MainScreen
import com.example.androidsampleapp.ui.sleep.SleepScreen

/**
 * アプリ全体の遷移。持つルートは 2 つだけで、タブの中身は [MainScreen] の内側の
 * NavHost が持つ。下部バーの有無で階層を分けている。
 */
@Composable
fun AppNavigation(
    appStateHolder: AppStateHolder,
    idleTimer: IdleTimer,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val isSleeping by appStateHolder.isSleeping.collectAsStateWithLifecycle()

    IncomingCallRouter(appStateHolder = appStateHolder, idleTimer = idleTimer)

    LaunchedEffect(isSleeping) {
        if (isSleeping) {
            navController.navigate(Route.SLEEP) { launchSingleTop = true }
        } else {
            navController.popBackStack(Route.MAIN, inclusive = false)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            // 子より先にイベントを覗いて無操作タイマーを戻す。子の操作は妨げない。
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial)
                        idleTimer.onInteraction()
                    }
                }
            },
    ) {
        NavHost(navController = navController, startDestination = Route.MAIN) {
            composable(Route.MAIN) {
                MainScreen(onNavigateToSleep = { appStateHolder.updateSleeping(true) })
            }
            composable(Route.SLEEP) {
                SleepScreen(onWake = { idleTimer.onInteraction() })
            }
        }
    }
}
