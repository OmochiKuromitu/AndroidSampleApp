package com.example.androidsampleapp.core

import com.example.androidsampleapp.domain.model.CallHistory
import com.example.androidsampleapp.domain.model.Contact
import com.example.androidsampleapp.domain.repository.ContactRepository
import com.example.androidsampleapp.domain.usecase.GetMissedCallCountUseCase
import com.example.androidsampleapp.domain.usecase.MarkMissedCallsAsReadUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MissedCallManagerTest {

    /** 呼ばれた回数を数える。既読にすると以後は 0 件を返す。 */
    private class FakeContactRepository(
        private val counts: List<Int> = listOf(1, 2, 3),
        private val failOnGet: () -> Boolean = { false },
    ) : ContactRepository {
        var getCount = 0
            private set
        var readCount = 0
            private set

        override suspend fun getContacts(): List<Contact> = emptyList()

        override suspend fun getCallHistories(): List<CallHistory> = emptyList()

        override suspend fun getMissedCallCount(): Int {
            if (failOnGet()) error("通信失敗")
            val index = getCount
            getCount++
            delay(100)
            return if (readCount > 0) 0 else counts.getOrElse(index) { counts.last() }
        }

        override suspend fun markMissedCallsAsRead() {
            readCount++
            delay(100)
        }
    }

    /**
     * 実行中の要求が終わるところまで仮想時間を進める。
     *
     * マネージャーは [TestScope.backgroundScope] で動かしている。そこで起動したコルーチンは
     * `advanceUntilIdle()` の待ち対象に入らず、偽リポジトリの `delay` を越えないまま戻ってくる。
     * そのため時間を明示して進める。
     */
    private fun TestScope.settle() {
        advanceTimeBy(SETTLE_MS)
        runCurrent()
    }

    private fun manager(repository: ContactRepository, scope: CoroutineScope) =
        MissedCallManager(
            getMissedCallCount = GetMissedCallCountUseCase(repository),
            markMissedCallsAsRead = MarkMissedCallsAsReadUseCase(repository),
            scope = scope,
        )

    @Test
    fun `refresh で件数が入る`() = runTest {
        val repository = FakeContactRepository()
        val manager = manager(repository, backgroundScope)

        manager.refresh()
        settle()

        assertEquals(1, manager.missedCallCount.value)
    }

    @Test
    fun `実行中に重ねて呼んでも要求は 1 本にまとまる`() = runTest {
        // タブを続けて叩かれたときに、同じ要求が重なって遅い順に上書きされるのを防ぐ。
        val repository = FakeContactRepository()
        val manager = manager(repository, backgroundScope)

        manager.refresh()
        manager.refresh()
        manager.refresh()
        settle()

        assertEquals(1, repository.getCount)
    }

    @Test
    fun `終わってから呼べば取り直す`() = runTest {
        val repository = FakeContactRepository()
        val manager = manager(repository, backgroundScope)

        manager.refresh()
        settle()
        manager.refresh()
        settle()

        assertEquals(2, repository.getCount)
        assertEquals(2, manager.missedCallCount.value)
    }

    @Test
    fun `markAsRead で既読にして件数が 0 になる`() = runTest {
        val repository = FakeContactRepository()
        val manager = manager(repository, backgroundScope)
        manager.refresh()
        settle()
        assertEquals(1, manager.missedCallCount.value)

        manager.markAsRead()
        settle()

        assertEquals(1, repository.readCount)
        assertEquals(0, manager.missedCallCount.value)
    }

    @Test
    fun `既読は取得中でも取りやめない`() = runTest {
        // 利用者の操作の結果なので、落とすと見たのにバッジが残る。
        val repository = FakeContactRepository()
        val manager = manager(repository, backgroundScope)

        manager.refresh()
        manager.markAsRead()
        settle()

        assertEquals(1, repository.readCount)
        assertEquals(0, manager.missedCallCount.value)
    }

    @Test
    fun `取得に失敗しても前回の件数を残す`() = runTest {
        // 通信が一度こけただけでバッジが消えると、不在着信を見落とす方に倒れる。
        var shouldFail = false
        val repository = FakeContactRepository(counts = listOf(5), failOnGet = { shouldFail })
        val manager = manager(repository, backgroundScope)

        manager.refresh()
        settle()
        assertEquals(5, manager.missedCallCount.value)

        shouldFail = true
        manager.refresh()
        settle()

        assertEquals(5, manager.missedCallCount.value)
    }

    private companion object {
        /** 偽リポジトリの遅延（100ms）を 2 回分越えられる長さ。 */
        const val SETTLE_MS = 1_000L
    }
}
