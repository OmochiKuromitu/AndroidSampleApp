package com.example.androidsampleapp.ui.top

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidsampleapp.R
import com.example.androidsampleapp.domain.usecase.AnswerCallUseCase
import com.example.androidsampleapp.domain.usecase.ObserveAirconStateUseCase
import com.example.androidsampleapp.domain.usecase.ObserveIncomingCallUseCase
import com.example.androidsampleapp.domain.usecase.RejectCallUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
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
) : ViewModel() {

    private val _uiState = MutableStateFlow(TopState())
    val uiState: StateFlow<TopState> = _uiState.asStateFlow()

    private val _effect = Channel<TopEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private val reducer = TopReducer()

    init {
        viewModelScope.launch {
            observeIncomingCall().collect { onIntent(TopIntent.IncomingCallChanged(it)) }
        }
        viewModelScope.launch {
            observeAircon().collect { onIntent(TopIntent.AirconChanged(it)) }
        }
    }

    /**
     * 状態を変えうる入力の入口。Route からの操作も、購読した値の変化も、通信の結果も、すべてここを通す。
     *
     * Reducer で次の状態を作って [_uiState] に入れ、そのあと [handle] で副作用を実行する。
     * 副作用は並行に走らせる。長い通信が、後から来た Intent の反映を止めないようにするため。
     */
    fun onIntent(intent: TopIntent) {
        var previous: TopState
        var current: TopState
        // 読んでから書くまでの間に別の更新が入っていたら、読み直してやり直す。
        do {
            previous = _uiState.value
            current = reducer.reduce(previous, intent)
        } while (!_uiState.compareAndSet(previous, current))
        viewModelScope.launch { handle(intent, previous, current) }
    }

    private suspend fun handle(intent: TopIntent, previous: TopState, current: TopState) {
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
            onIntent(TopIntent.CommandFailed)
            return
        }
        runCatching { send(roomId) }
            .onSuccess {
                onIntent(TopIntent.CommandSucceeded)
                _effect.send(TopEffect.ShowMessage(successMessage))
            }
            .onFailure {
                onIntent(TopIntent.CommandFailed)
                _effect.send(TopEffect.ShowMessage(R.string.command_failed))
            }
    }
}
