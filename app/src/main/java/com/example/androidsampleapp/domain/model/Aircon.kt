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
 * 運転モード。機器とやり取りする文字列との対応は data 層（data/CodeMapping）が、
 * 表示名は ui 層（ui/common/Labels）が持つ。ドメインは機器の言葉も画面の言葉も知らない。
 */
enum class AirconMode {
    COOL,
    HEAT,
    DRY,
    FAN,
}
