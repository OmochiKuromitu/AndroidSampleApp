package com.example.androidsampleapp.model

/**
 * 機器仕様に由来する固定値。サーバから取得するようになったら、
 * ここを Repository 経由の取得に差し替える。
 */
object MasterData {
    const val MIN_TEMPERATURE = 16.0
    const val MAX_TEMPERATURE = 30.0
    const val TEMPERATURE_STEP = 0.5

    /** 機器が受け付ける運転モードのコード。 */
    val airconModeCodes = listOf("COOL", "HEAT", "DRY", "FAN")

    fun clampTemperature(value: Double): Double =
        value.coerceIn(MIN_TEMPERATURE, MAX_TEMPERATURE)
}
