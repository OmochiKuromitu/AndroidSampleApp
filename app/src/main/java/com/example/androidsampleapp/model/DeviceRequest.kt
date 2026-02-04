package com.example.androidsampleapp.model

import kotlinx.serialization.Serializable

/**
 * どの端末からの要求かだけを名乗る本文。状態の取得と通知の取得・消去で使う。
 *
 * 項目名はサンプル用の仮の仕様。サーバが `device_id` なら `@SerialName` を付ける。
 */
@Serializable
data class DeviceRequest(
    val deviceId: String,
)

/** エアコンの運転を切り替える本文。 */
@Serializable
data class AirconPowerRequest(
    val deviceId: String,
    val isOn: Boolean,
)

/** エアコンの運転モードを変える本文。[mode] は `COOL` などの文字列（data/CodeMapping）。 */
@Serializable
data class AirconModeRequest(
    val deviceId: String,
    val mode: String,
)

/** エアコンの設定温度を変える本文。 */
@Serializable
data class AirconTemperatureRequest(
    val deviceId: String,
    val targetTemperature: Double,
)

/** 着信に応答する、または拒否する本文。 */
@Serializable
data class CallRequest(
    val deviceId: String,
    val roomId: String,
)
