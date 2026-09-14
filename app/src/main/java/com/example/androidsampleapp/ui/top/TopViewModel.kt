package com.example.androidsampleapp.ui.top

import androidx.lifecycle.viewModelScope
import com.example.androidsampleapp.R
import com.example.androidsampleapp.core.mvi.MviViewModel
import com.example.androidsampleapp.domain.usecase.AnswerCallUseCase
import com.example.androidsampleapp.domain.usecase.ObserveAirconStateUseCase
import com.example.androidsampleapp.domain.usecase.ObserveIncomingCallUseCase
import com.example.androidsampleapp.domain.usecase.RejectCallUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * トップ画面の ViewModel。
 *
 * - 着信とエアコンの状態を購読し、Intent にして Reducer に流す。
 * - 応答・拒否の Intent を受けたら、[handle] で UseCase を通してコマンドを送り、
 *   結果を Intent で戻してスナックバーを Effect で出す。
 */
@HiltViewModel
class TopViewModel @Inject constructor(
    observeIncomingCall: ObserveIncomingCallUseCase,
    observeAircon: ObserveAirconStateUseCase,
    private val answerCall: AnswerCallUseCase,
    private val rejectCall: RejectCallUseCase,
) : MviViewModel<TopState, TopIntent, TopEffect>(
    initialState = TopState(),
    reducer = TopReducer(),
) {

    init {
        viewModelScope.launch {
            observeIncomingCall().collect { dispatch(TopIntent.IncomingCallChanged(it)) }
        }
        viewModelScope.launch {
            observeAircon().collect { dispatch(TopIntent.AirconChanged(it)) }
        }
    }

    override suspend fun handle(intent: TopIntent, previous: TopState, current: TopState) {
        when (intent) {
            TopIntent.AnswerClicked -> respondToCall(
                roomId = previous.incomingCall?.roomId,
                successMessage = R.string.top_answered,
            ) { answerCall(it) }

            TopIntent.RejectClicked -> respondToCall(
                roomId = previous.incomingCall?.roomId,
                successMessage = R.string.top_rejected,
            ) { rejectCall(it) }

            is TopIntent.IncomingCallChanged,
            is TopIntent.AirconChanged,
            TopIntent.CommandSucceeded,
            TopIntent.CommandFailed,
            -> Unit
        }
    }

    private suspend fun respondToCall(
        roomId: String?,
        successMessage: Int,
        send: suspend (String) -> Unit,
    ) {
        if (roomId == null) {
            dispatch(TopIntent.CommandFailed)
            return
        }
        runCatching { send(roomId) }
            .onSuccess {
                dispatch(TopIntent.CommandSucceeded)
                sendEffect(TopEffect.ShowMessage(successMessage))
            }
            .onFailure {
                dispatch(TopIntent.CommandFailed)
                sendEffect(TopEffect.ShowMessage(R.string.command_failed))
            }
    }
}
