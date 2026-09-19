package com.example.androidsampleapp.network

import com.example.androidsampleapp.model.DeviceMessage
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TCP で受け取った 1 行を [DeviceMessage] にする。
 * 通信も状態も持たない純粋な変換なので、JVM テストで検証できる。
 */
@Singleton
class MessageParser @Inject constructor() {

    fun parse(raw: String): DeviceMessage {
        val parts = raw.trim().split(DELIMITER)
        return when (parts.firstOrNull()) {
            "CALL" -> parts.getOrNull(1)?.let { roomId ->
                DeviceMessage.CallStarted(
                    roomId = roomId,
                    displayName = parts.getOrNull(2).orEmpty().ifBlank { roomId },
                )
            } ?: DeviceMessage.Unknown(raw)

            "CALL_END" -> DeviceMessage.CallEnded

            "AIRCON" -> {
                val target = parts.getOrNull(3)?.toDoubleOrNull()
                val room = parts.getOrNull(4)?.toDoubleOrNull()
                if (target == null || room == null) {
                    DeviceMessage.Unknown(raw)
                } else {
                    DeviceMessage.AirconStatus(
                        isOn = parts.getOrNull(1).equals("ON", ignoreCase = true),
                        mode = parts.getOrNull(2).orEmpty(),
                        targetTemperature = target,
                        roomTemperature = room,
                    )
                }
            }

            // 詳細が無い通知は出しても意味が無いので Unknown にする。タイトルは空でよい。
            "NOTICE" -> parts.getOrNull(3)?.takeIf { it.isNotBlank() }?.let { message ->
                DeviceMessage.NoticeReceived(
                    category = parts.getOrNull(1).orEmpty(),
                    title = parts.getOrNull(2)?.takeIf { it.isNotBlank() },
                    message = message,
                    destination = parts.getOrNull(4).orEmpty(),
                )
            } ?: DeviceMessage.Unknown(raw)

            "PONG" -> DeviceMessage.Pong

            else -> DeviceMessage.Unknown(raw)
        }
    }

    private companion object {
        const val DELIMITER = "|"
    }
}
