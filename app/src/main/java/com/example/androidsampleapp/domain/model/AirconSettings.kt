package com.example.androidsampleapp.domain.model

/**
 * このアプリで最後に設定に成功した温度とモード。未設定の項目はデフォルト値を使う。
 * Repository がプロセス内のメモリに保持し、機器の現在値とは別に扱う。
 */
data class AirconSettings(
    val targetTemperature: Double = 26.0,
    val mode: AirconMode = AirconMode.COOL,
)
