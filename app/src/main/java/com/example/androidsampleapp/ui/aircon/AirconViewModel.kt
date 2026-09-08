package com.example.androidsampleapp.ui.aircon

import androidx.lifecycle.viewModelScope
import com.example.androidsampleapp.R
import com.example.androidsampleapp.core.mvi.MviViewModel
import com.example.androidsampleapp.domain.usecase.ObserveAirconStateUseCase
import com.example.androidsampleapp.domain.usecase.ObserveConnectionStateUseCase
import com.example.androidsampleapp.domain.usecase.SetAirconModeUseCase
import com.example.androidsampleapp.domain.usecase.SetAirconPowerUseCase
import com.example.androidsampleapp.domain.usecase.SetAirconTemperatureUseCase
import com.example.androidsampleapp.model.MasterData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class AirconViewModel @Inject constructor(
    observeAircon: ObserveAirconStateUseCase,
    observeConnectionState: ObserveConnectionStateUseCase,
    private val setPower: SetAirconPowerUseCase,
    private val setMode: SetAirconModeUseCase,
    private val setTemperature: SetAirconTemperatureUseCase,
) : MviViewModel<AirconState, AirconIntent, AirconEffect>(
    initialState = AirconState(),
    reducer = AirconReducer(),
) {

    init {
        viewModelScope.launch {
            observeAircon().collect { dispatch(AirconIntent.AirconChanged(it)) }
        }
        viewModelScope.launch {
            observeConnectionState().collect { dispatch(AirconIntent.ConnectionStateChanged(it)) }
        }
    }

    override suspend fun handle(intent: AirconIntent, previous: AirconState, current: AirconState) {
        when (intent) {
            is AirconIntent.PowerToggled -> send { setPower(intent.isOn) }

            AirconIntent.TemperatureUpClicked ->
                send { setTemperature(previous.aircon.targetTemperature + MasterData.TEMPERATURE_STEP) }

            AirconIntent.TemperatureDownClicked ->
                send { setTemperature(previous.aircon.targetTemperature - MasterData.TEMPERATURE_STEP) }

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
            .onSuccess { dispatch(AirconIntent.CommandSucceeded) }
            .onFailure {
                dispatch(AirconIntent.CommandFailed)
                sendEffect(AirconEffect.ShowMessage(R.string.command_failed))
            }
    }
}
