package com.example.androidsampleapp.ui.top

import com.example.androidsampleapp.domain.model.IncomingCall
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TopReducerTest {

    private val reducer = TopReducer()

    @Test
    fun `着信を取り込む`() {
        val call = IncomingCall(roomId = "101", displayName = "玄関")

        val next = reducer.reduce(TopState(), TopIntent.IncomingCallChanged(call))

        assertEquals(call, next.incomingCall)
    }

    @Test
    fun `応答すると送信中になる`() {
        val next = reducer.reduce(TopState(), TopIntent.AnswerClicked)

        assertTrue(next.isSendingCommand)
    }

    @Test
    fun `送信が終われば送信中が解ける`() {
        val next = reducer.reduce(TopState(isSendingCommand = true), TopIntent.CommandSucceeded)

        assertFalse(next.isSendingCommand)
    }

    @Test
    fun `着信が消えたら null になる`() {
        val state = TopState(incomingCall = IncomingCall("101", "玄関"))

        val next = reducer.reduce(state, TopIntent.IncomingCallChanged(null))

        assertNull(next.incomingCall)
    }
}
