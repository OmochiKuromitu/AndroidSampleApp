package com.example.androidsampleapp.ui.aircon

import com.example.androidsampleapp.core.mvi.UiIntent
import com.example.androidsampleapp.domain.model.Aircon
import com.example.androidsampleapp.domain.model.AirconMode
import com.example.androidsampleapp.domain.model.ConnectionState

sealed interface AirconIntent : UiIntent {
    data class AirconChanged(val aircon: Aircon) : AirconIntent
    data class ConnectionStateChanged(val state: ConnectionState) : AirconIntent

    data class PowerToggled(val isOn: Boolean) : AirconIntent
    data object TemperatureUpClicked : AirconIntent
    data object TemperatureDownClicked : AirconIntent
    data class ModeSelected(val mode: AirconMode) : AirconIntent

    data object CommandSucceeded : AirconIntent
    data object CommandFailed : AirconIntent
}
