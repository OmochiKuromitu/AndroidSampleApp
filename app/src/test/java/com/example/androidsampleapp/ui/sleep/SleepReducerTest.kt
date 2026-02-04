package com.example.androidsampleapp.ui.sleep

import com.example.androidsampleapp.core.NoticeSnapshot
import com.example.androidsampleapp.domain.model.Notice
import com.example.androidsampleapp.domain.model.NoticeCategory
import com.example.androidsampleapp.domain.model.NoticeDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepReducerTest {

    private val reducer = SleepReducer()

    private val notices = listOf(
        notice(id = "1", occurredAt = 100L),
    )

    private fun notice(id: String, occurredAt: Long) =
        Notice(id, NoticeCategory.CALL, null, "玄関から呼び出し", occurredAt, NoticeDestination.Top)

    @Test
    fun `一覧の変化で通知と読み込み状態が入る`() {
        val next = reducer.reduce(
            SleepState(isLoadingNotices = true),
            SleepIntent.NoticesChanged(
                NoticeSnapshot(notices = notices, isLoaded = true, loadFailed = true),
            ),
        )

        assertEquals(notices, next.notices)
        assertFalse(next.isLoadingNotices)
        assertTrue(next.noticeLoadFailed)
    }

    @Test
    fun `まだ受け取っていないスナップショットなら読み込み中になる`() {
        val next = reducer.reduce(SleepState(), SleepIntent.NoticesChanged(NoticeSnapshot()))

        assertTrue(next.isLoadingNotices)
    }

    @Test
    fun `消去を押した時点では確認ダイアログを出すだけで消さない`() {
        val state = SleepState(notices = notices)

        val next = reducer.reduce(state, SleepIntent.ClearNoticesClicked)

        assertEquals(state.copy(isClearConfirmVisible = true), next)
    }

    @Test
    fun `はいを押したらダイアログを閉じる。読み込み中は先読みしない`() {
        // 読み込み中かどうかは MissedCallManager が決めて NoticesChanged で戻る。
        val state = SleepState(notices = notices, isClearConfirmVisible = true)

        val next = reducer.reduce(state, SleepIntent.ClearNoticesConfirmed)

        assertEquals(state.copy(isClearConfirmVisible = false), next)
    }

    @Test
    fun `いいえで閉じたら何も起きない`() {
        val state = SleepState(notices = notices, isClearConfirmVisible = true)

        val next = reducer.reduce(state, SleepIntent.ClearNoticesDismissed)

        assertEquals(state.copy(isClearConfirmVisible = false), next)
    }

    @Test
    fun `通知のタップは状態を変えない`() {
        val state = SleepState(notices = notices)

        val next = reducer.reduce(state, SleepIntent.NoticeClicked(notices.first()))

        assertEquals(state, next)
    }

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
        val next = reducer.reduce(SleepState(unlockProgress = 0.8f), SleepIntent.UnlockCancelled)

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
