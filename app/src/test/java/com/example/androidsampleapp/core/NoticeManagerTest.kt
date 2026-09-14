package com.example.androidsampleapp.core

import com.example.androidsampleapp.domain.model.ConnectionState
import com.example.androidsampleapp.domain.model.IncomingCall
import com.example.androidsampleapp.domain.model.Notice
import com.example.androidsampleapp.domain.model.NoticeCategory
import com.example.androidsampleapp.domain.model.NoticeDestination
import com.example.androidsampleapp.domain.repository.DeviceRepository
import com.example.androidsampleapp.domain.repository.NoticeRepository
import com.example.androidsampleapp.domain.usecase.ClearNoticesUseCase
import com.example.androidsampleapp.domain.usecase.GetNoticesUseCase
import com.example.androidsampleapp.domain.usecase.ObserveDeviceNoticesUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NoticeManagerTest {

    /** 呼ばれた回数を数える。消去すると以後は空を返す。 */
    private class FakeNoticeRepository(
        private val notices: List<Notice>,
        private val failOnGet: () -> Boolean = { false },
    ) : NoticeRepository {
        var getCount = 0
            private set
        var deleteCount = 0
            private set

        override suspend fun getNotices(): List<Notice> {
            getCount++
            delay(100)
            if (failOnGet()) error("通信失敗")
            return if (deleteCount > 0) emptyList() else notices
        }

        override suspend fun deleteAllNotices() {
            deleteCount++
            delay(100)
        }
    }

    private class FakeDeviceRepository : DeviceRepository {
        override val connectionState: StateFlow<ConnectionState> =
            MutableStateFlow(ConnectionState.CONNECTED)
        override val incomingCall: StateFlow<IncomingCall?> = MutableStateFlow(null)
        override val deviceNotices = MutableStateFlow<List<Notice>>(emptyList())

        override suspend fun monitor() = Unit
        override suspend fun answerCall(roomId: String) = Unit
        override suspend fun rejectCall(roomId: String) = Unit

        override fun clearDeviceNotices() {
            deviceNotices.value = emptyList()
        }
    }

    private fun notice(id: String, occurredAt: Long, category: NoticeCategory = NoticeCategory.INFO) =
        Notice(id, category, null, "本文", occurredAt, NoticeDestination.Top)

    private fun manager(
        repository: NoticeRepository,
        device: DeviceRepository,
        scope: CoroutineScope,
    ) = NoticeManager(
        getNotices = GetNoticesUseCase(repository),
        clearNotices = ClearNoticesUseCase(repository, device),
        observeDeviceNotices = ObserveDeviceNoticesUseCase(device),
        scope = scope,
    )

    /** 実行中の要求が終わるところまで仮想時間を進める。理由は MissedCallManagerTest を参照。 */
    private fun TestScope.settle() {
        advanceTimeBy(SETTLE_MS)
        runCurrent()
    }

    @Test
    fun `refresh で API の一覧が入る`() = runTest {
        val api = listOf(notice("1", 100L))
        val manager = manager(FakeNoticeRepository(api), FakeDeviceRepository(), backgroundScope)

        manager.refresh()
        settle()

        assertEquals(api, manager.snapshot.value.notices)
        assertFalse(manager.snapshot.value.isLoading)
    }

    @Test
    fun `取得中は読み込み中になる`() = runTest {
        val manager = manager(FakeNoticeRepository(emptyList()), FakeDeviceRepository(), backgroundScope)

        manager.refresh()
        runCurrent()

        assertTrue(manager.snapshot.value.isLoading)
    }

    @Test
    fun `API と機器の通知を新しい順に合わせる`() = runTest {
        val device = FakeDeviceRepository()
        val manager = manager(
            FakeNoticeRepository(listOf(notice("api-new", 300L), notice("api-old", 100L))),
            device,
            backgroundScope,
        )

        manager.refresh()
        device.deviceNotices.value = listOf(notice("device-mid", 200L))
        settle()

        assertEquals(
            listOf("api-new", "device-mid", "api-old"),
            manager.snapshot.value.notices.map { it.id },
        )
    }

    @Test
    fun `機器からの通知は取りに行かなくても届く`() = runTest {
        val device = FakeDeviceRepository()
        val repository = FakeNoticeRepository(emptyList())
        val manager = manager(repository, device, backgroundScope)

        device.deviceNotices.value = listOf(notice("device-1", 100L))
        settle()

        assertEquals(listOf("device-1"), manager.snapshot.value.notices.map { it.id })
        assertEquals(0, repository.getCount)
    }

    @Test
    fun `実行中に重ねて呼んでも要求は 1 本にまとまる`() = runTest {
        val repository = FakeNoticeRepository(emptyList())
        val manager = manager(repository, FakeDeviceRepository(), backgroundScope)

        manager.refresh()
        manager.refresh()
        manager.refresh()
        settle()

        assertEquals(1, repository.getCount)
    }

    @Test
    fun `取得に失敗しても前回の一覧を残して失敗を立てる`() = runTest {
        var shouldFail = false
        val api = listOf(notice("1", 100L))
        val manager = manager(
            FakeNoticeRepository(api, failOnGet = { shouldFail }),
            FakeDeviceRepository(),
            backgroundScope,
        )
        manager.refresh()
        settle()

        shouldFail = true
        manager.refresh()
        settle()

        assertEquals(api, manager.snapshot.value.notices)
        assertTrue(manager.snapshot.value.loadFailed)
        assertFalse(manager.snapshot.value.isLoading)
    }

    @Test
    fun `消去は取得中でも取りやめず、機器からの通知も消える`() = runTest {
        // 利用者の操作の結果なので、落とすと消したのに残る。
        val repository = FakeNoticeRepository(listOf(notice("1", 100L)))
        val device = FakeDeviceRepository()
        device.deviceNotices.value = listOf(notice("device-1", 200L))
        val manager = manager(repository, device, backgroundScope)

        manager.refresh()
        manager.clear()
        settle()

        assertEquals(1, repository.deleteCount)
        assertTrue(manager.snapshot.value.notices.isEmpty())
        assertFalse(manager.snapshot.value.loadFailed)
    }

    private companion object {
        /** 偽リポジトリの遅延（100ms）を消去と取り直しの 2 回分越えられる長さ。 */
        const val SETTLE_MS = 1_000L
    }
}
