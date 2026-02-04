package com.example.androidsampleapp.ui.main

import com.example.androidsampleapp.domain.model.ConnectionState
import org.junit.Assert.assertEquals
import org.junit.Test

class MainReducerTest {

    private val reducer = MainReducer()

    @Test
    fun `接続状態の変化を取り込む`() {
        val next = reducer.reduce(
            MainState(),
            MainIntent.ConnectionStateChanged(ConnectionState.CONNECTED),
        )

        assertEquals(ConnectionState.CONNECTED, next.connectionState)
    }

    @Test
    fun `不在着信の件数を取り込む`() {
        val next = reducer.reduce(MainState(), MainIntent.MissedCallCountChanged(3))

        assertEquals(3, next.missedCallCount)
    }

    @Test
    fun `切断も取り込む`() {
        val state = MainState(connectionState = ConnectionState.CONNECTED)

        val next = reducer.reduce(
            state,
            MainIntent.ConnectionStateChanged(ConnectionState.DISCONNECTED),
        )

        assertEquals(ConnectionState.DISCONNECTED, next.connectionState)
    }
}
