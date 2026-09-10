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
        val message = parser.parse("NOTICE|ALERT|フィルターの清掃時期です|AIRCON")

        assertEquals(
            DeviceMessage.NoticeReceived(
                category = "ALERT",
                message = "フィルターの清掃時期です",
                destination = "AIRCON",
            ),
            message,
        )
    }

    @Test
    fun `本文の無い通知は Unknown にする`() {
        assertTrue(parser.parse("NOTICE|ALERT") is DeviceMessage.Unknown)
    }

    @Test
    fun `知らない行は Unknown にする`() {
        val message = parser.parse("HELLO")

        assertEquals(DeviceMessage.Unknown("HELLO"), message)
    }
}
