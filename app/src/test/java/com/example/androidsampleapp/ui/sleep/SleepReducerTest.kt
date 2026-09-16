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
        notice(id = "1", occurredAt = 100L),
    )

    private fun notice(id: String, occurredAt: Long) =
        Notice(id, NoticeCategory.CALL, null, "玄関から呼び出し", occurredAt, NoticeDestination.Top)

    @Test
    fun `初めは API の通知を受け取るまで読み込み中`() {
        assertTrue(SleepState().isLoadingNotices)
    }

    @Test
    fun `Started で前回の失敗が消えて読み込み中に戻る`() {
        val next = reducer.reduce(SleepState(noticeLoadFailed = true), SleepIntent.Started)

        assertTrue(next.isLoadingNotices)
        assertFalse(next.noticeLoadFailed)
    }

    @Test
    fun `受け取り済みなら消去を押しても読み込み中にしない`() {
        // 取り直しの間も今の一覧を出したままにする。
        val state = SleepState(apiNotices = notices, isApiNoticesLoaded = true, noticeLoadFailed = true)

        val next = reducer.reduce(state, SleepIntent.ClearNoticesClicked)

        assertFalse(next.isLoadingNotices)
        assertFalse(next.noticeLoadFailed)
        assertEquals(notices, next.notices)
    }

    @Test
    fun `API の通知を受け取ったら一覧が入り読み込み中が解ける`() {
        val next = reducer.reduce(SleepState(), SleepIntent.ApiNoticesChanged(notices))

        assertEquals(notices, next.notices)
        assertFalse(next.isLoadingNotices)
    }

    @Test
    fun `取ったら空だった場合も読み込み中が解ける`() {
        // 「まだ取っていない空」と区別できないと、空のときに読み込み中のまま止まる。
        val next = reducer.reduce(SleepState(), SleepIntent.ApiNoticesChanged(emptyList()))

        assertTrue(next.notices.isEmpty())
        assertFalse(next.isLoadingNotices)
    }

    @Test
    fun `機器からの通知が入る`() {
        val device = listOf(notice(id = "device-1", occurredAt = 200L))

        val next = reducer.reduce(SleepState(), SleepIntent.DeviceNoticesChanged(device))

        assertEquals(device, next.notices)
    }

    @Test
    fun `API を取り直しても機器からの通知は残る`() {
        val device = listOf(notice(id = "device-1", occurredAt = 200L))

        val next = reducer.reduce(
            SleepState(deviceNotices = device),
            SleepIntent.ApiNoticesChanged(emptyList()),
        )

        assertEquals(device, next.notices)
    }

    @Test
    fun `機器からの通知が変わっても API の一覧は残る`() {
        val next = reducer.reduce(
            SleepState(apiNotices = notices),
            SleepIntent.DeviceNoticesChanged(emptyList()),
        )

        assertEquals(notices, next.notices)
    }

    @Test
    fun `出どころの違う通知は新しい順に混ざる`() {
        val state = SleepState(
            apiNotices = listOf(notice("api-new", 300L), notice("api-old", 100L)),
            deviceNotices = listOf(notice("device-mid", 200L)),
        )

        assertEquals(listOf("api-new", "device-mid", "api-old"), state.notices.map { it.id })
    }

    @Test
    fun `取得に失敗したら読み込み中が解けて失敗が立つ`() {
        val next = reducer.reduce(SleepState(), SleepIntent.NoticesLoadFailed)

        assertFalse(next.isLoadingNotices)
        assertTrue(next.noticeLoadFailed)
    }

    @Test
    fun `取得に失敗しても受け取り済みの一覧は残す`() {
        val state = SleepState(apiNotices = notices, isApiNoticesLoaded = true)

        val next = reducer.reduce(state, SleepIntent.NoticesLoadFailed)

        assertEquals(notices, next.notices)
        assertTrue(next.noticeLoadFailed)
    }

    @Test
    fun `通知のタップは状態を変えない`() {
        val state = SleepState(apiNotices = notices)

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
