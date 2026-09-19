package com.example.androidsampleapp.core

import com.example.androidsampleapp.domain.model.AddressBook
import com.example.androidsampleapp.domain.repository.ContactRepository
import com.example.androidsampleapp.domain.usecase.GetMissedCallCountUseCase
import com.example.androidsampleapp.domain.usecase.MarkMissedCallsAsReadUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
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

        override val addressBook: StateFlow<AddressBook?> = MutableStateFlow(null)

        override suspend fun refreshAddressBook() = Unit

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
        advanceUntilIdle()

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
        advanceUntilIdle()

        assertEquals(1, repository.getCount)
    }

    @Test
    fun `終わってから呼べば取り直す`() = runTest {
        val repository = FakeContactRepository()
        val manager = manager(repository, backgroundScope)

        manager.refresh()
        advanceUntilIdle()
        manager.refresh()
        advanceUntilIdle()

        assertEquals(2, repository.getCount)
        assertEquals(2, manager.missedCallCount.value)
    }

    @Test
    fun `markAsRead で既読にして件数が 0 になる`() = runTest {
        val repository = FakeContactRepository()
        val manager = manager(repository, backgroundScope)
        manager.refresh()
        advanceUntilIdle()
        assertEquals(1, manager.missedCallCount.value)

        manager.markAsRead()
        advanceUntilIdle()

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
        advanceUntilIdle()

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
        advanceUntilIdle()
        assertEquals(5, manager.missedCallCount.value)

        shouldFail = true
        manager.refresh()
        advanceUntilIdle()

        assertEquals(5, manager.missedCallCount.value)
    }
}
