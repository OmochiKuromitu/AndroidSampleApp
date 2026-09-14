package com.example.androidsampleapp.ui.aircon

import com.example.androidsampleapp.core.mvi.UiIntent
import com.example.androidsampleapp.domain.model.Aircon
import com.example.androidsampleapp.domain.model.AirconMode
import com.example.androidsampleapp.domain.model.ConnectionState

/**
 * エアコン画面の状態を変えうる入力の一覧。
 *
 * 利用者の操作だけでなく、機器から降ってくる状態の変化と、コマンド送信の結果も Intent にする。
 * どれも Reducer を通すことで、状態が変わる入口を 1 つに保つ。
 */
sealed interface AirconIntent : UiIntent {
    // ---- 機器から降ってくる状態の変化（ViewModel が購読して投げる）

    /** エアコンの現在値が変わった。 */
    data class AirconChanged(val aircon: Aircon) : AirconIntent

    /** 機器との接続状態が変わった。未接続の間は操作を止める。 */
    data class ConnectionStateChanged(val state: ConnectionState) : AirconIntent

    // ---- 利用者の操作（Route が Screen のコールバックから投げる）

    /** 運転のスイッチを切り替えた。[isOn] は切り替えた後の値。 */
    data class PowerToggled(val isOn: Boolean) : AirconIntent

    /** 設定温度を 1 段上げるボタンを押した。上げ幅は MasterData が持つ。 */
    data object TemperatureUpClicked : AirconIntent

    /** 設定温度を 1 段下げるボタンを押した。 */
    data object TemperatureDownClicked : AirconIntent

    /** 運転モードを選んだ。 */
    data class ModeSelected(val mode: AirconMode) : AirconIntent

    // ---- コマンド送信の結果（ViewModel が送信後に投げる）

    /**
     * コマンドを送り終えた。機器が受け付けたかどうかではない（UDP なので到達確認はしない）。
     * 実際の値は機器からの [AirconChanged] で反映される。
     */
    data object CommandSucceeded : AirconIntent

    /** コマンドを送れなかった。スナックバーは Effect で別に出す。 */
    data object CommandFailed : AirconIntent
}
