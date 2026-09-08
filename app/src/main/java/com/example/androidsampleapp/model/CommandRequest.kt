package com.example.androidsampleapp.model

/**
 * UDP で機器へ送るコマンド。文字列化の責務をここに持たせ、
 * UdpCommandClient は「渡された文字列を送る」だけにする。
 */
sealed interface CommandRequest {
    fun encode(): String

    data class AnswerCall(val roomId: String) : CommandRequest {
        override fun encode(): String = "CALL_ANSWER|$roomId"
    }

    data class RejectCall(val roomId: String) : CommandRequest {
        override fun encode(): String = "CALL_REJECT|$roomId"
    }

    data class SetAirconPower(val isOn: Boolean) : CommandRequest {
        override fun encode(): String = "AIRCON_POWER|" + if (isOn) "ON" else "OFF"
    }

    data class SetAirconMode(val mode: String) : CommandRequest {
        override fun encode(): String = "AIRCON_MODE|$mode"
    }

    data class SetAirconTemperature(val value: Double) : CommandRequest {
        override fun encode(): String = "AIRCON_TEMP|$value"
    }
}
