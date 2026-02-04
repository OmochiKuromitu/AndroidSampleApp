package com.example.androidsampleapp.ui.aircon

import com.example.androidsampleapp.core.mvi.UiState
import com.example.androidsampleapp.domain.model.Aircon
import com.example.androidsampleapp.domain.model.ConnectionState

/**
 * エアコン画面の状態。機器そのものの値は [Aircon]（domain/model）を持ち、
 * 送信中フラグのような画面都合の値だけをここに足す。
 */
data class AirconState(
    val aircon: Aircon = Aircon(),
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val isSendingCommand: Boolean = false,
) : UiState {
    /** 未接続、または送信中は操作を受け付けない。 */
    val isOperable: Boolean
        get() = connectionState.isConnected && !isSendingCommand
}
