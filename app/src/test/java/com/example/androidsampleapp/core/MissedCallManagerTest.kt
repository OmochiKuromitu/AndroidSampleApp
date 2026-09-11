package com.example.androidsampleapp.core

import com.example.androidsampleapp.domain.model.CallHistory
import com.example.androidsampleapp.domain.model.Contact
import com.example.androidsampleapp.domain.repository.ContactRepository
import com.example.androidsampleapp.domain.usecase.GetMissedCallCountUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MissedCallManagerTest {

    /** 呼ばれた回数を数え、毎回違う件数を返す。 */
    private class FakeContactRepository(
        private val counts: List<Int> = listOf(1, 2, 3),
    ) : ContactRepository {
        var callCount = 0
            private set

        override suspend fun getContacts(): List<Contact> = emptyList()

        override suspend fun getCallHistories(): List<CallHistory> = emptyList()

        override suspend fun getMissedCallCount(): Int {
            val index = callCount
            callCount++
            delay(100)
            return counts.getOrElse(index) { counts.last() }
        }
    }

    @Test
    fun `refresh で件数が入る`() = runTest {
        val repository = FakeContactRepository()
        val manager = MissedCallManager(GetMissedCallCountUseCase(repository), backgroundScope)

        manager.refresh()
        advanceUntilIdle()

        assertEquals(1, manager.missedCallCount.value)
    }

    @Test
    fun `実行中に重ねて呼んでも要求は 1 本にまとまる`() = runTest {
        // タブを続けて叩かれたときに、同じ要求が重なって遅い順に上書きされるのを防ぐ。
        val repository = FakeContactRepository()
        val manager = MissedCallManager(GetMissedCallCountUseCase(repository), backgroundScope)

        manager.refresh()
        manager.refresh()
        manager.refresh()
        advanceUntilIdle()

        assertEquals(1, repository.callCount)
    }

    @Test
    fun `終わってから呼べば取り直す`() = runTest {
        val repository = FakeContactRepository()
        val manager = MissedCallManager(GetMissedCallCountUseCase(repository), backgroundScope)

        manager.refresh()
        advanceUntilIdle()
        manager.refresh()
        advanceUntilIdle()

        assertEquals(2, repository.callCount)
        assertEquals(2, manager.missedCallCount.value)
    }

    @Test
    fun `失敗しても前回の件数を残す`() = runTest {
        // 通信が一度こけただけでバッジが消えると、不在着信を見落とす方に倒れる。
        var shouldFail = false
        val repository = object : ContactRepository {
            override suspend fun getContacts(): List<Contact> = emptyList()
            override suspend fun getCallHistories(): List<CallHistory> = emptyList()
            override suspend fun getMissedCallCount(): Int {
                if (shouldFail) error("通信失敗")
                return 5
            }
        }
        val manager = MissedCallManager(GetMissedCallCountUseCase(repository), backgroundScope)

        manager.refresh()
        advanceUntilIdle()
        shouldFail = true
        manager.refresh()
        advanceUntilIdle()

        assertEquals(5, manager.missedCallCount.value)
    }
}
