package com.example.androidsampleapp.data

import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationRepositoryStateTest {

    @Test
    fun `重複した件数取得は間引き、完了後は取り直す`() = runTest {
        val fixture = NotificationTestFixture()
        val repository = fixture.contactRepository
        repeat(3) { launch { repository.refreshMissedCallCount() } }
        advanceUntilIdle()

        assertEquals(listOf("count"), fixture.contactApi.calls)
        assertEquals(5, repository.missedCallCount.value)
        assertSame(fixture.manager.missedCallCount, repository.missedCallCount)

        fixture.contactApi.count = 7
        repository.refreshMissedCallCount()
        assertEquals(7, repository.missedCallCount.value)
        assertEquals(2, fixture.contactApi.calls.size)
    }

    @Test
    fun `件数取得と既読の失敗では前回の件数を残す`() = runTest {
        val fixture = NotificationTestFixture()
        val repository = fixture.contactRepository
        repository.refreshMissedCallCount()

        fixture.contactApi.failOnGet = true
        assertTrue(runCatching { repository.refreshMissedCallCount() }.exceptionOrNull() is IOException)
        assertEquals(5, repository.missedCallCount.value)

        fixture.contactApi.failOnRead = true
        assertTrue(runCatching { repository.markMissedCallsAsRead() }.exceptionOrNull() is IOException)
        assertEquals(5, repository.missedCallCount.value)

        fixture.contactApi.failOnGet = false
        fixture.contactApi.failOnRead = false
        repository.markMissedCallsAsRead()
        assertEquals(0, repository.missedCallCount.value)
    }

    @Test
    fun `取得中の既読は待って実行し、操作と再取得の間に別の取得を入れない`() = runTest {
        val fixture = NotificationTestFixture()
        val repository = fixture.contactRepository
        fixture.contactApi.countAfterRead = 1
        launch { repository.refreshMissedCallCount() }
        runCurrent()
        launch { repository.markMissedCallsAsRead() }
        runCurrent()
        assertEquals(listOf("count"), fixture.contactApi.calls)

        advanceUntilIdle()
        assertEquals(listOf("count", "read", "count"), fixture.contactApi.calls)
        assertEquals(1, repository.missedCallCount.value)

        launch { repository.markMissedCallsAsRead() }
        runCurrent()
        repository.refreshMissedCallCount()
        advanceUntilIdle()
        assertEquals(listOf("count", "read", "count", "read", "count"), fixture.contactApi.calls)
    }

    @Test
    fun `重なった既読操作も取り落とさない`() = runTest {
        val fixture = NotificationTestFixture()
        repeat(2) { launch { fixture.contactRepository.markMissedCallsAsRead() } }
        advanceUntilIdle()
        assertEquals(listOf("read", "count", "read", "count"), fixture.contactApi.calls)
    }

    @Test
    fun `重複した通知取得は間引き、一覧を新しい順で共有する`() = runTest {
        val fixture = NotificationTestFixture()
        val repository = fixture.noticeRepository
        fixture.noticeApi.notices = listOf(noticeResponse("old", 100), noticeResponse("new", 300))
        repeat(3) { launch { repository.refreshNotices() } }
        advanceUntilIdle()

        assertEquals(listOf("list"), fixture.noticeApi.calls)
        assertEquals(listOf("new", "old"), repository.notices.value.map { it.id })
        assertSame(fixture.manager.notices, repository.notices)
    }

    @Test
    fun `取得に失敗したら一覧を空にし、次に取れたら一覧が入る`() = runTest {
        val fixture = NotificationTestFixture()
        val repository = fixture.noticeRepository
        assertTrue(repository.notices.value.isEmpty())
        repository.refreshNotices()
        assertEquals(listOf("old"), repository.notices.value.map { it.id })

        fixture.noticeApi.failOnGet = true
        assertTrue(runCatching { repository.refreshNotices() }.exceptionOrNull() is IOException)
        assertTrue(repository.notices.value.isEmpty())

        fixture.noticeApi.failOnGet = false
        repository.refreshNotices()
        assertEquals(listOf("old"), repository.notices.value.map { it.id })
    }

    @Test
    fun `取得済みなら取り直し中も前回の一覧を表示する`() = runTest {
        val fixture = NotificationTestFixture()
        val repository = fixture.noticeRepository
        repository.refreshNotices()
        val previous = repository.notices.value
        launch { repository.refreshNotices() }
        runCurrent()

        assertEquals(previous, repository.notices.value)
        advanceUntilIdle()
    }

    @Test
    fun `取得中の消去も実行し、再取得では新着通知を残す`() = runTest {
        val fixture = NotificationTestFixture()
        val repository = fixture.noticeRepository
        fixture.noticeApi.noticesAfterDelete = listOf(noticeResponse("new", 300))
        launch { repository.refreshNotices() }
        runCurrent()
        launch { repository.deleteAllNotices() }
        runCurrent()
        assertEquals(listOf("list"), fixture.noticeApi.calls)
        advanceUntilIdle()

        assertEquals(listOf("list", "delete", "list"), fixture.noticeApi.calls)
        assertEquals(listOf("new"), repository.notices.value.map { it.id })

        launch { repository.deleteAllNotices() }
        runCurrent()
        repository.refreshNotices()
        advanceUntilIdle()
        assertEquals(listOf("list", "delete", "list", "delete", "list"), fixture.noticeApi.calls)
    }

    @Test
    fun `消去に失敗したら一覧を空にし、取り直さない`() = runTest {
        val fixture = NotificationTestFixture()
        val repository = fixture.noticeRepository
        repository.refreshNotices()
        fixture.noticeApi.failOnDelete = true
        assertTrue(runCatching { repository.deleteAllNotices() }.exceptionOrNull() is IOException)

        assertEquals(listOf("list", "delete"), fixture.noticeApi.calls)
        assertTrue(repository.notices.value.isEmpty())
    }

    @Test
    fun `重なった消去操作も取り落とさない`() = runTest {
        val fixture = NotificationTestFixture()
        repeat(2) { launch { fixture.noticeRepository.deleteAllNotices() } }
        advanceUntilIdle()
        assertEquals(listOf("delete", "list", "delete", "list"), fixture.noticeApi.calls)
    }

    @Test
    fun `種類の異なる操作は互いの取得を中断しない`() = runTest {
        val fixture = NotificationTestFixture()
        launch { fixture.contactRepository.refreshMissedCallCount() }
        launch { fixture.noticeRepository.deleteAllNotices() }
        advanceUntilIdle()
        assertEquals(5, fixture.contactRepository.missedCallCount.value)
        assertEquals(listOf("delete", "list"), fixture.noticeApi.calls)

        fixture.noticeApi.notices = listOf(noticeResponse("new", 300))
        launch { fixture.noticeRepository.refreshNotices() }
        launch { fixture.contactRepository.markMissedCallsAsRead() }
        advanceUntilIdle()
        assertEquals(0, fixture.contactRepository.missedCallCount.value)
        assertEquals(listOf("new"), fixture.noticeRepository.notices.value.map { it.id })
    }

    @Test
    fun `キャンセルは取得失敗にせず一覧を空にしない。ロックを解放して再取得できる`() = runTest {
        val fixture = NotificationTestFixture()
        fixture.noticeRepository.refreshNotices()
        val countJob = launch { fixture.contactRepository.refreshMissedCallCount() }
        val noticeJob = launch { fixture.noticeRepository.refreshNotices() }
        runCurrent()
        countJob.cancelAndJoin()
        noticeJob.cancelAndJoin()

        assertEquals(0, fixture.contactRepository.missedCallCount.value)
        assertEquals(listOf("old"), fixture.noticeRepository.notices.value.map { it.id })
        fixture.noticeApi.notices = listOf(noticeResponse("new", 300))
        fixture.contactRepository.refreshMissedCallCount()
        fixture.noticeRepository.refreshNotices()
        assertEquals(5, fixture.contactRepository.missedCallCount.value)
        assertEquals(listOf("new"), fixture.noticeRepository.notices.value.map { it.id })
    }

    @Test
    fun `API がキャンセル後に結果を返しても共有状態を上書きしない`() = runTest {
        val fixture = NotificationTestFixture()
        fixture.contactRepository.refreshMissedCallCount()
        fixture.noticeRepository.refreshNotices()
        val previous = fixture.noticeRepository.notices.value
        fixture.contactApi.countResponse = {
            withContext(NonCancellable) { delay(100); 99 }
        }
        fixture.noticeApi.listResponse = {
            withContext(NonCancellable) { delay(100); emptyList() }
        }
        val countJob = launch { fixture.contactRepository.refreshMissedCallCount() }
        val noticeJob = launch { fixture.noticeRepository.refreshNotices() }
        runCurrent()
        countJob.cancel()
        noticeJob.cancel()
        advanceUntilIdle()

        assertEquals(5, fixture.contactRepository.missedCallCount.value)
        assertEquals(previous, fixture.noticeRepository.notices.value)
    }
}
