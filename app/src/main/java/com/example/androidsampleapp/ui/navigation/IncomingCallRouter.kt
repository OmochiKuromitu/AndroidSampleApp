package com.example.androidsampleapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidsampleapp.domain.model.IncomingCall
import kotlinx.coroutines.flow.StateFlow

/**
 * 着信を検知して知らせるだけ。何をするかは呼び出し側（[AppNavigation]）が決める。
 *
 * 検知と遷移を分けておくと、着信時の挙動を変えるときにこのファイルを触らずに済む。
 */
@Composable
fun IncomingCallRouter(
    incomingCall: StateFlow<IncomingCall?>,
    onIncomingCall: () -> Unit,
) {
    val current by incomingCall.collectAsStateWithLifecycle()

    LaunchedEffect(current) {
        if (current != null) onIncomingCall()
    }
}
