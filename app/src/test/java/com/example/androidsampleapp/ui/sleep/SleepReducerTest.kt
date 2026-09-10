package com.example.androidsampleapp.ui.sleep

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepReducerTest {

    private val reducer = SleepReducer()

    @Test
    fun `スワイプの進み具合が入る`() {
        val next = reducer.reduce(SleepState(), SleepIntent.UnlockDragged(0.4f))

        assertEquals(0.4f, next.unlockProgress, TOLERANCE)
        assertFalse(next.isUnlockReached)
    }

    @Test
    fun `必要な距離に届いたら解除とみなす`() {
        val next = reducer.reduce(SleepState(), SleepIntent.UnlockDragged(1f))

        assertTrue(next.isUnlockReached)
    }

    @Test
    fun `進み具合は 0f から 1f に収める`() {
        assertEquals(
            1f,
            reducer.reduce(SleepState(), SleepIntent.UnlockDragged(2.5f)).unlockProgress,
            TOLERANCE,
        )
        assertEquals(
            0f,
            reducer.reduce(SleepState(), SleepIntent.UnlockDragged(-1f)).unlockProgress,
            TOLERANCE,
        )
    }

    @Test
    fun `途中で指を離すと進み具合が戻る`() {
        val state = SleepState(unlockProgress = 0.8f)

        val next = reducer.reduce(state, SleepIntent.UnlockCancelled)

        assertEquals(0f, next.unlockProgress, TOLERANCE)
    }

    @Test
    fun `時刻の更新は進み具合に触らない`() {
        val state = SleepState(unlockProgress = 0.5f)

        val next = reducer.reduce(state, SleepIntent.Ticked("12:34", "9月10日 (水)"))

        assertEquals(0.5f, next.unlockProgress, TOLERANCE)
        assertEquals("12:34", next.timeText)
    }

    private companion object {
        const val TOLERANCE = 0.0001f
    }
}
