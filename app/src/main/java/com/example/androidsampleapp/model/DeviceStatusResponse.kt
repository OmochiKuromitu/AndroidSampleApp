package com.example.androidsampleapp.model

import kotlinx.serialization.Serializable

/**
 * 状態取得 API のレスポンス。定期的に取りに行き、接続状態・着信・エアコンの現在値に反映する。
 *
 * サーバの語彙のまま受け取り、ドメインの型への変換は data 層が行う。
 * 形はサンプル用の仮の仕様。
 */
@Serializable
data class DeviceStatusResponse(
    val aircon: AirconStatusResponse,
    /** 着信が無ければ null（項目ごと省いてもよい）。 */
    val incomingCall: IncomingCallResponse? = null,
)

/** エアコンの現在値。状態取得 API と、エアコン操作 API の応答で返る。 */
@Serializable
data class AirconStatusResponse(
    val isOn: Boolean,
    /** `COOL` などの文字列。対応は data/CodeMapping。 */
    val mode: String,
    val targetTemperature: Double,
    val roomTemperature: Double,
)

/** 着信 1 件。 */
@Serializable
data class IncomingCallResponse(
    val roomId: String,
    val displayName: String,
)
