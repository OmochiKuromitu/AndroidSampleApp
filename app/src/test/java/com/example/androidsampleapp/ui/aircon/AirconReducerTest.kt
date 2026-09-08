package com.example.androidsampleapp.ui.aircon

import com.example.androidsampleapp.domain.model.Aircon
import com.example.androidsampleapp.domain.model.AirconMode
import com.example.androidsampleapp.domain.model.ConnectionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AirconReducerTest {

    private val reducer = AirconReducer()

    @Test
    fun `操作すると送信中になる`() {
        val next = reducer.reduce(AirconState(), AirconIntent.PowerToggled(true))

        assertTrue(next.isSendingCommand)
    }

    @Test
    fun `送信中は電源状態をまだ変えない`() {
        // 実際の値は機器からの通知で入る。楽観的に更新しないことで表示と実機のずれを防ぐ。
        val next = reducer.reduce(AirconState(), AirconIntent.PowerToggled(true))

        assertFalse(next.aircon.isOn)
    }

    @Test
    fun `機器からの通知で値が入る`() {
        val aircon = Aircon(isOn = true, mode = AirconMode.HEAT, targetTemperature = 22.0)

        val next = reducer.reduce(
            AirconState(isSendingCommand = true),
            AirconIntent.AirconChanged(aircon),
        )

        assertEquals(aircon, next.aircon)
    }

    @Test
    fun `未接続なら操作できない`() {
        val state = AirconState(connectionState = ConnectionState.DISCONNECTED)

        assertFalse(state.isOperable)
    }

    @Test
    fun `接続中かつ送信中でなければ操作できる`() {
        val state = AirconState(
            connectionState = ConnectionState.CONNECTED,
            isSendingCommand = false,
        )

        assertTrue(state.isOperable)
    }
}
