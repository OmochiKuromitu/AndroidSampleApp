package com.example.androidsampleapp.ui.main

import com.example.androidsampleapp.core.mvi.UiIntent
import com.example.androidsampleapp.domain.model.ConnectionState

sealed interface MainIntent : UiIntent {
    data class ConnectionStateChanged(val state: ConnectionState) : MainIntent
    data class MissedCallCountChanged(val count: Int) : MainIntent
}
