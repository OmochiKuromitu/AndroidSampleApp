package com.example.androidsampleapp.ui.aircon

import com.example.androidsampleapp.core.mvi.UiState
import com.example.androidsampleapp.domain.model.Aircon
import com.example.androidsampleapp.domain.model.AirconSettings
import com.example.androidsampleapp.domain.model.ConnectionState

/**
 * エアコン画面の状態。機器の現在値と、Repository が保持する温度・モードの設定値を受け取る。
 * 設定値は画面を開き直しても復元し、電源・室温は機器の現在値を表示する。
 */
data class AirconState(
    val aircon: Aircon = Aircon(),
    val settings: AirconSettings = AirconSettings(),
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val isSendingCommand: Boolean = false,
) : UiState {
    /** 未接続、または送信中は操作を受け付けない。 */
    val isOperable: Boolean
        get() = connectionState.isConnected && !isSendingCommand
}
