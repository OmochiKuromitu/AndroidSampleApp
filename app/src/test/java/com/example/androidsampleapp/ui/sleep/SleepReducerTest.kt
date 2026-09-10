package com.example.androidsampleapp.ui.sleep

import com.example.androidsampleapp.domain.model.Notice
import com.example.androidsampleapp.domain.model.NoticeCategory
import com.example.androidsampleapp.domain.model.NoticeDestination
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

    @Test
    fun `受け取った通知が入る`() {
        val notices = listOf(
            Notice("1", NoticeCategory.CALL, "玄関から呼び出し", NoticeDestination.TOP),
        )

        val next = reducer.reduce(SleepState(), SleepIntent.NoticesChanged(notices))

        assertEquals(notices, next.notices)
    }

    @Test
    fun `消去を押した時点では一覧を書き換えない`() {
        // 実際に消えたかどうかは NoticesChanged で戻ってくる。先読みしない。
        val state = SleepState(
            notices = listOf(
                Notice("1", NoticeCategory.CALL, "玄関から呼び出し", NoticeDestination.TOP),
            ),
        )

        val next = reducer.reduce(state, SleepIntent.ClearNoticesClicked)

        assertEquals(state, next)
    }

    @Test
    fun `通知のタップは状態を変えない`() {
        val notice = Notice("1", NoticeCategory.AIRCON, "設定温度を変更", NoticeDestination.AIRCON)
        val state = SleepState(notices = listOf(notice))

        val next = reducer.reduce(state, SleepIntent.NoticeClicked(notice))

        assertEquals(state, next)
    }

    private companion object {
        const val TOLERANCE = 0.0001f
    }
}
