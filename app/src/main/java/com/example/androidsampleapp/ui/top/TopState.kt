package com.example.androidsampleapp.ui.top

import com.example.androidsampleapp.core.mvi.UiState
import com.example.androidsampleapp.domain.model.Aircon
import com.example.androidsampleapp.domain.model.IncomingCall

data class TopState(
    val incomingCall: IncomingCall? = null,
    val aircon: Aircon = Aircon(),
    val isSendingCommand: Boolean = false,
) : UiState
