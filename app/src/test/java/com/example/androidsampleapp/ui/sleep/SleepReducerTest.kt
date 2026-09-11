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

    private val notices = listOf(
        Notice("1", NoticeCategory.CALL, "玄関から呼び出し", NoticeDestination.TOP),
    )

    @Test
    fun `Started で通知の取得中になる`() {
        val next = reducer.reduce(SleepState(noticeLoadFailed = true), SleepIntent.Started)

        assertTrue(next.isLoadingNotices)
        assertFalse(next.noticeLoadFailed)
    }

    @Test
    fun `消去を押したときも取得中になる`() {
        // 消去は取り直しまで含む 1 つの操作なので、取得と同じ扱いにする。
        val next = reducer.reduce(SleepState(), SleepIntent.ClearNoticesClicked)

        assertTrue(next.isLoadingNotices)
    }

    @Test
    fun `取得できた一覧が入る`() {
        val next = reducer.reduce(
            SleepState(isLoadingNotices = true),
            SleepIntent.NoticesLoaded(notices),
        )

        assertEquals(notices, next.notices)
        assertFalse(next.isLoadingNotices)
    }

    @Test
    fun `消去後の空の一覧も同じ経路で入る`() {
        val next = reducer.reduce(
            SleepState(notices = notices, isLoadingNotices = true),
            SleepIntent.NoticesLoaded(emptyList()),
        )

        assertTrue(next.notices.isEmpty())
        assertFalse(next.isLoadingNotices)
    }

    @Test
    fun `取得に失敗したら取得中が解けて失敗が立つ`() {
        val next = reducer.reduce(
            SleepState(isLoadingNotices = true),
            SleepIntent.NoticesLoadFailed,
        )

        assertFalse(next.isLoadingNotices)
        assertTrue(next.noticeLoadFailed)
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
