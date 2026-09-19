package com.example.androidsampleapp.network

import com.example.androidsampleapp.model.DeviceMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageParserTest {

    private val parser = MessageParser()

    @Test
    fun `着信を解釈する`() {
        val message = parser.parse("CALL|101|玄関")

        assertEquals(DeviceMessage.CallStarted("101", "玄関"), message)
    }

    @Test
    fun `表示名が無ければ部屋番号で代替する`() {
        val message = parser.parse("CALL|101")

        assertEquals(DeviceMessage.CallStarted("101", "101"), message)
    }

    @Test
    fun `エアコン状態を解釈する`() {
        val message = parser.parse("AIRCON|ON|COOL|26.0|28.4")

        assertEquals(
            DeviceMessage.AirconStatus(
                isOn = true,
                mode = "COOL",
                targetTemperature = 26.0,
                roomTemperature = 28.4,
            ),
            message,
        )
    }

    @Test
    fun `温度が数値でなければ Unknown にする`() {
        val message = parser.parse("AIRCON|ON|COOL|あつい|28.4")

        assertTrue(message is DeviceMessage.Unknown)
    }

    @Test
    fun `通知を解釈する`() {
        val message = parser.parse("NOTICE|ALERT|非常ボタン|集会室の非常ボタンが押されました|TOP")

        assertEquals(
            DeviceMessage.NoticeReceived(
                category = "ALERT",
                title = "非常ボタン",
                message = "集会室の非常ボタンが押されました",
                destination = "TOP",
            ),
            message,
        )
    }

    @Test
    fun `通知のタイトルが空なら null にする`() {
        val message = parser.parse("NOTICE|INFO||システムを起動しました|TOP")

        assertEquals(null, (message as DeviceMessage.NoticeReceived).title)
    }

    @Test
    fun `詳細の無い通知は Unknown にする`() {
        val message = parser.parse("NOTICE|ALERT|非常ボタン||TOP")

        assertTrue(message is DeviceMessage.Unknown)
    }

    @Test
    fun `知らない行は Unknown にする`() {
        val message = parser.parse("HELLO")

        assertEquals(DeviceMessage.Unknown("HELLO"), message)
    }
}
