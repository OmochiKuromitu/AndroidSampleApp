package com.example.androidsampleapp.ui.top

import com.example.androidsampleapp.core.mvi.Reducer

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
