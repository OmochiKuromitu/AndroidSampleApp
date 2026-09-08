package com.example.androidsampleapp.ui.main

import com.example.androidsampleapp.core.mvi.UiIntent
import com.example.androidsampleapp.domain.model.ConnectionState

sealed interface MainIntent : UiIntent {
    /** 下部バーがタップされた。 */
    data class TabClicked(val tab: MainTab) : MainIntent

    /** 戻る操作などで NavController 側の現在地が変わった。State を追従させるだけ。 */
    data class BackStackChanged(val tab: MainTab) : MainIntent

    data class ConnectionStateChanged(val state: ConnectionState) : MainIntent

    /** 着信したらトップへ引き戻す。 */
    data object IncomingCallReceived : MainIntent
}
