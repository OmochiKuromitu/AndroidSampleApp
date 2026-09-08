package com.example.androidsampleapp.ui.sleep

import com.example.androidsampleapp.core.mvi.UiIntent
import com.example.androidsampleapp.domain.model.ConnectionState

sealed interface SleepIntent : UiIntent {
    data class Ticked(val timeText: String, val dateText: String) : SleepIntent
    data class ConnectionStateChanged(val state: ConnectionState) : SleepIntent
    data object ScreenTapped : SleepIntent
}
