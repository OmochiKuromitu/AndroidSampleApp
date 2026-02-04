package com.example.androidsampleapp.ui.aircon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidsampleapp.R
import com.example.androidsampleapp.domain.model.AirconSpec
import com.example.androidsampleapp.domain.usecase.ObserveAirconStateUseCase
import com.example.androidsampleapp.domain.usecase.ObserveConnectionStateUseCase
import com.example.androidsampleapp.domain.usecase.SetAirconModeUseCase
import com.example.androidsampleapp.domain.usecase.SetAirconPowerUseCase
import com.example.androidsampleapp.domain.usecase.SetAirconTemperatureUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * エアコン画面の ViewModel。
 *
 * - 機器のエアコン状態と接続状態を購読し、Intent にして Reducer に流す。
 * - 操作の Intent を受けたら、[handle] で UseCase を通してコマンドを送り、結果を Intent で戻す。
 *   失敗したときはスナックバーを Effect で出す。
 */
@HiltViewModel
class AirconViewModel @Inject constructor(
    observeAircon: ObserveAirconStateUseCase,
    observeConnectionState: ObserveConnectionStateUseCase,
    private val setPower: SetAirconPowerUseCase,
    private val setMode: SetAirconModeUseCase,
    private val setTemperature: SetAirconTemperatureUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AirconState())
    val uiState: StateFlow<AirconState> = _uiState.asStateFlow()

    private val _effect = Channel<AirconEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private val reducer = AirconReducer()

    init {
        viewModelScope.launch {
            observeAircon().collect { onIntent(AirconIntent.AirconChanged(it)) }
        }
        viewModelScope.launch {
            observeConnectionState().collect { onIntent(AirconIntent.ConnectionStateChanged(it)) }
        }
    }

    /**
     * 状態を変えうる入力の入口。Route からの操作も、購読した値の変化も、通信の結果も、すべてここを通す。
     *
     * Reducer で次の状態を作って [_uiState] に入れ、そのあと [handle] で副作用を実行する。
     * 副作用は並行に走らせる。長い通信が、後から来た Intent の反映を止めないようにするため。
     */
    fun onIntent(intent: AirconIntent) {
        var previous: AirconState
        var current: AirconState
        // 読んでから書くまでの間に別の更新が入っていたら、読み直してやり直す。
        do {
            previous = _uiState.value
            current = reducer.reduce(previous, intent)
        } while (!_uiState.compareAndSet(previous, current))
        viewModelScope.launch { handle(intent, previous, current) }
    }

    private suspend fun handle(intent: AirconIntent, previous: AirconState, current: AirconState) {
        when (intent) {
            is AirconIntent.PowerToggled -> send { setPower(intent.isOn) }

            AirconIntent.TemperatureUpClicked ->
                send { setTemperature(previous.aircon.targetTemperature + AirconSpec.TEMPERATURE_STEP) }

            AirconIntent.TemperatureDownClicked ->
                send { setTemperature(previous.aircon.targetTemperature - AirconSpec.TEMPERATURE_STEP) }

            is AirconIntent.ModeSelected -> send { setMode(intent.mode) }

            is AirconIntent.AirconChanged,
            is AirconIntent.ConnectionStateChanged,
            AirconIntent.CommandSucceeded,
            AirconIntent.CommandFailed,
            -> Unit
        }
    }

    private suspend fun send(command: suspend () -> Unit) {
        runCatching { command() }
            .onSuccess { onIntent(AirconIntent.CommandSucceeded) }
            .onFailure {
                onIntent(AirconIntent.CommandFailed)
                _effect.send(AirconEffect.ShowMessage(R.string.command_failed))
            }
    }
}
