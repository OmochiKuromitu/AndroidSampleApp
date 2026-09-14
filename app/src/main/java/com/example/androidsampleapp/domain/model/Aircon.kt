package com.example.androidsampleapp.domain.model

/**
 * エアコンの現在値。画面の状態（ui/aircon/AirconState）と区別するため、
 * ドメイン側は機器そのものを指す名前にしている。
 */
data class Aircon(
    val isOn: Boolean = false,
    val mode: AirconMode = AirconMode.COOL,
    val targetTemperature: Double = 26.0,
    val roomTemperature: Double = 26.0,
)

/**
 * 運転モード。機器とやり取りする文字列との対応は data 層（data/CodeMapping）が持ち、
 * ドメインは機器の言葉を知らない。
 */
enum class AirconMode(val label: String) {
    COOL("冷房"),
    HEAT("暖房"),
    DRY("除湿"),
    FAN("送風"),
}
