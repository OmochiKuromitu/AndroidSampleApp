package com.example.androidsampleapp.ui.top

import com.example.androidsampleapp.core.mvi.Reducer

/**
 * トップ画面の (状態, Intent) -> 次の状態。
 *
 * 純粋関数に保つ。応答・拒否のコマンド送信とスナックバーは TopViewModel.handle が行う。
 */
class TopReducer : Reducer<TopState, TopIntent> {
    override fun reduce(state: TopState, intent: TopIntent): TopState = when (intent) {
        is TopIntent.IncomingCallChanged -> state.copy(incomingCall = intent.call)
        is TopIntent.AirconChanged -> state.copy(aircon = intent.aircon)

        TopIntent.AnswerClicked,
        TopIntent.RejectClicked,
        -> state.copy(isSendingCommand = true)

        TopIntent.CommandSucceeded,
        TopIntent.CommandFailed,
        -> state.copy(isSendingCommand = false)
    }
}
