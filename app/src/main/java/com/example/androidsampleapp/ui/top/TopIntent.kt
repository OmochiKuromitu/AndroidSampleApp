package com.example.androidsampleapp.ui.top

import com.example.androidsampleapp.core.mvi.UiIntent
import com.example.androidsampleapp.domain.model.Aircon
import com.example.androidsampleapp.domain.model.IncomingCall

sealed interface TopIntent : UiIntent {
    /** 共有状態の変化も Intent として受け取り、Reducer を唯一の入口に保つ。 */
    data class IncomingCallChanged(val call: IncomingCall?) : TopIntent
    data class AirconChanged(val aircon: Aircon) : TopIntent

    data object AnswerClicked : TopIntent
    data object RejectClicked : TopIntent

    data object CommandSucceeded : TopIntent
    data object CommandFailed : TopIntent
}
