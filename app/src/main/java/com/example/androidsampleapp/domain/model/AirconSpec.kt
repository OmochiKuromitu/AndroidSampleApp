package com.example.androidsampleapp.domain.model

/**
 * エアコンが受け付ける設定温度の範囲と刻み。
 *
 * 画面（刻み幅）と UseCase（範囲への丸め）の両方が使うので domain に置く。
 * サーバから取得するようになったら、ここを Repository 経由の取得に差し替える。
 */
object AirconSpec {
    const val MIN_TEMPERATURE = 16.0
    const val MAX_TEMPERATURE = 30.0
    const val TEMPERATURE_STEP = 0.5

    fun clampTemperature(value: Double): Double =
        value.coerceIn(MIN_TEMPERATURE, MAX_TEMPERATURE)
}
