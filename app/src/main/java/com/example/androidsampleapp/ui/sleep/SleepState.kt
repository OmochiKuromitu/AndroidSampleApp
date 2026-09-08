package com.example.androidsampleapp.ui.sleep

import com.example.androidsampleapp.core.mvi.UiState
import com.example.androidsampleapp.domain.model.ConnectionState

data class SleepState(
    val timeText: String = "",
    val dateText: String = "",
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
) : UiState
