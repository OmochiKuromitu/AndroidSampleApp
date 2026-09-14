package com.example.androidsampleapp.ui.aircon

import com.example.androidsampleapp.core.mvi.Reducer

/**
 * エアコン画面の (状態, Intent) -> 次の状態。
 *
 * 純粋関数に保つ。コマンドの送信やスナックバーは AirconViewModel.handle が行う。
 */
class AirconReducer : Reducer<AirconState, AirconIntent> {
    override fun reduce(state: AirconState, intent: AirconIntent): AirconState = when (intent) {
        is AirconIntent.AirconChanged -> state.copy(aircon = intent.aircon)
        is AirconIntent.ConnectionStateChanged -> state.copy(connectionState = intent.state)

        // 送信結果は機器からの通知で反映されるので、ここでは送信中にするだけ。
        is AirconIntent.PowerToggled,
        AirconIntent.TemperatureUpClicked,
        AirconIntent.TemperatureDownClicked,
        is AirconIntent.ModeSelected,
        -> state.copy(isSendingCommand = true)

        AirconIntent.CommandSucceeded,
        AirconIntent.CommandFailed,
        -> state.copy(isSendingCommand = false)
    }
}
