package com.example.androidsampleapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidsampleapp.core.AppStateHolder

/**
 * 着信したときの画面の振り分け。
 *
 * ここでは「スリープからの復帰」だけを担う。着信でトップタブへ切り替える判断は
 * MainViewModel が MainIntent.IncomingCallReceived として持っている。
 * スリープはアプリ全体の状態、タブは Main 画面の状態なので、置き場所を分けている。
 */
@Composable
fun IncomingCallRouter(
    appStateHolder: AppStateHolder,
    idleTimer: IdleTimer,
) {
    val incomingCall by appStateHolder.incomingCall.collectAsStateWithLifecycle()

    LaunchedEffect(incomingCall) {
        if (incomingCall != null) idleTimer.onInteraction()
    }
}
