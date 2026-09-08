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

enum class AirconMode(val code: String, val label: String) {
    COOL("COOL", "冷房"),
    HEAT("HEAT", "暖房"),
    DRY("DRY", "除湿"),
    FAN("FAN", "送風"),
    ;

    companion object {
        fun fromCode(code: String): AirconMode =
            entries.firstOrNull { it.code == code } ?: COOL
    }
}
