package com.example.androidsampleapp.model

/**
 * TCP で受信した 1 行を解釈した結果。ここは通信の語彙のままにしておき、
 * 画面が使う型（domain/model）への変換は data 層で行う。
 */
sealed interface DeviceMessage {
    data class CallStarted(val roomId: String, val displayName: String) : DeviceMessage
    data object CallEnded : DeviceMessage
    data class AirconStatus(
        val isOn: Boolean,
        val mode: String,
        val targetTemperature: Double,
        val roomTemperature: Double,
    ) : DeviceMessage

    /**
     * 機器からの通知。`NOTICE|種別|タイトル|詳細|飛び先`。
     * TODO: 機器側の仕様が決まったら形式を合わせる。今は仮置き。
     */
    data class NoticeReceived(
        val category: String,
        val title: String?,
        val message: String,
        val destination: String,
    ) : DeviceMessage

    data object Pong : DeviceMessage
    data class Unknown(val raw: String) : DeviceMessage
}
