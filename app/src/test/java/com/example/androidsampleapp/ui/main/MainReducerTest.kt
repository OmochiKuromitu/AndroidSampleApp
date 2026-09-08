package com.example.androidsampleapp.ui.main

import com.example.androidsampleapp.domain.model.ConnectionState
import org.junit.Assert.assertEquals
import org.junit.Test

class MainReducerTest {

    private val reducer = MainReducer()

    @Test
    fun `タブをタップすると選択中タブが変わる`() {
        val next = reducer.reduce(MainState(), MainIntent.TabClicked(MainTab.AIRCON))

        assertEquals(MainTab.AIRCON, next.selectedTab)
    }

    @Test
    fun `スリープをタップしても選択中タブは動かない`() {
        val state = MainState(selectedTab = MainTab.AIRCON)

        val next = reducer.reduce(state, MainIntent.TabClicked(MainTab.SLEEP))

        assertEquals(MainTab.AIRCON, next.selectedTab)
    }

    @Test
    fun `着信するとトップに戻る`() {
        val state = MainState(selectedTab = MainTab.AIRCON)

        val next = reducer.reduce(state, MainIntent.IncomingCallReceived)

        assertEquals(MainTab.TOP, next.selectedTab)
    }

    @Test
    fun `接続状態の変化を取り込む`() {
        val next = reducer.reduce(
            MainState(),
            MainIntent.ConnectionStateChanged(ConnectionState.CONNECTED),
        )

        assertEquals(ConnectionState.CONNECTED, next.connectionState)
    }
}
