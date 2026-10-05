package com.example.androidsampleapp.ui.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import com.example.androidsampleapp.data.NotificationTestFixture
import com.example.androidsampleapp.data.noticeResponse
import com.example.androidsampleapp.domain.usecase.ClearNoticesUseCase
import com.example.androidsampleapp.domain.usecase.MarkMissedCallsAsReadUseCase
import com.example.androidsampleapp.domain.usecase.ObserveAddressBookUseCase
import com.example.androidsampleapp.domain.usecase.ObserveMissedCallCountUseCase
import com.example.androidsampleapp.domain.usecase.ObserveNoticesUseCase
import com.example.androidsampleapp.domain.usecase.RefreshAddressBookUseCase
import com.example.androidsampleapp.domain.usecase.RefreshMissedCallCountUseCase
import com.example.androidsampleapp.domain.usecase.RefreshNoticesUseCase
import com.example.androidsampleapp.ui.contact.ContactIntent
import com.example.androidsampleapp.ui.contact.ContactList
import com.example.androidsampleapp.ui.contact.ContactViewModel
import com.example.androidsampleapp.ui.sleep.SleepIntent
import com.example.androidsampleapp.ui.sleep.SleepViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SharedNotificationsViewModelTest {
    private lateinit var store: ViewModelStore

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        store = ViewModelStore()
    }

    @After
    fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    private fun navigationViewModel(fixture: NotificationTestFixture) = keep(
        MissedCallViewModel(
            RefreshMissedCallCountUseCase(fixture.contactRepository),
            RefreshNoticesUseCase(fixture.noticeRepository),
        ),
    )

    private fun contactViewModel(fixture: NotificationTestFixture) = keep(
        ContactViewModel(
            savedStateHandle = SavedStateHandle(),
            observeAddressBook = ObserveAddressBookUseCase(fixture.contactRepository),
            observeMissedCallCount = ObserveMissedCallCountUseCase(fixture.contactRepository),
            observeNotices = ObserveNoticesUseCase(fixture.noticeRepository),
            refreshAddressBook = RefreshAddressBookUseCase(fixture.contactRepository),
            markMissedCallsAsRead = MarkMissedCallsAsReadUseCase(fixture.contactRepository),
            clearNotices = ClearNoticesUseCase(fixture.noticeRepository),
        ),
    )

    private fun sleepViewModel(fixture: NotificationTestFixture) = keep(
        SleepViewModel(
            ObserveNoticesUseCase(fixture.noticeRepository),
            ClearNoticesUseCase(fixture.noticeRepository),
        ),
    )

    private fun <T : ViewModel> keep(viewModel: T): T {
        store.put(viewModel.javaClass.name, viewModel)
        return viewModel
    }

    @Test
    fun `画面遷移の取得は独立し、一方の失敗でも他方の結果が届く`() = runTest {
        val fixture = NotificationTestFixture()
        val viewModel = navigationViewModel(fixture)
        fixture.contactApi.failOnGet = true
        viewModel.refresh()
        advanceUntilIdle()
        assertEquals(0, fixture.contactRepository.missedCallCount.value)
        assertTrue(fixture.noticeRepository.noticeSnapshot.value.isLoaded)

        fixture.contactApi.failOnGet = false
        fixture.noticeApi.failOnGet = true
        viewModel.refresh()
        advanceUntilIdle()
        assertEquals(5, fixture.contactRepository.missedCallCount.value)
        assertTrue(fixture.noticeRepository.noticeSnapshot.value.loadFailed)
        assertEquals(listOf("old"), fixture.noticeRepository.noticeSnapshot.value.notices.map { it.id })
    }

    @Test
    fun `共通 ViewModel の破棄で両方の取得をキャンセルし、新しい ViewModel で再取得できる`() = runTest {
        val fixture = NotificationTestFixture()
        val viewModel = navigationViewModel(fixture)
        viewModel.refresh()
        runCurrent()
        store.clear()
        advanceUntilIdle()

        assertEquals(0, fixture.manager.missedCallCount.value)
        assertFalse(fixture.manager.noticeSnapshot.value.isLoaded)
        assertFalse(fixture.manager.noticeSnapshot.value.loadFailed)

        navigationViewModel(fixture).refresh()
        advanceUntilIdle()
        assertEquals(5, fixture.manager.missedCallCount.value)
        assertTrue(fixture.manager.noticeSnapshot.value.isLoaded)
    }

    @Test
    fun `共有結果は購読 UseCase から両画面の Reducer に届き、消去結果も一致する`() = runTest {
        val fixture = NotificationTestFixture()
        val navigation = navigationViewModel(fixture)
        val contact = contactViewModel(fixture)
        val sleep = sleepViewModel(fixture)
        navigation.refresh()
        advanceTimeBy(150)
        runCurrent()
        assertEquals(5, contact.uiState.value.missedCallCount)
        assertEquals(listOf("old"), contact.uiState.value.notices.map { it.id })
        assertEquals(contact.uiState.value.notices, sleep.uiState.value.notices)

        fixture.noticeApi.noticesAfterDelete = listOf(noticeResponse("new", 300))
        contact.onIntent(ContactIntent.ClearNoticesConfirmed)
        advanceTimeBy(250)
        runCurrent()
        assertEquals(listOf("new"), contact.uiState.value.notices.map { it.id })
        assertEquals(contact.uiState.value.notices, sleep.uiState.value.notices)

        contact.onIntent(ContactIntent.ListSelected(ContactList.HISTORY))
        advanceTimeBy(250)
        runCurrent()
        assertEquals(0, contact.uiState.value.missedCallCount)
        store.clear()
    }

    @Test
    fun `スリープ画面の消去失敗も両画面の状態に反映する`() = runTest {
        val fixture = NotificationTestFixture()
        val contact = contactViewModel(fixture)
        val sleep = sleepViewModel(fixture)
        fixture.noticeRepository.refreshNotices()
        fixture.noticeApi.failOnDelete = true
        sleep.onIntent(SleepIntent.ClearNoticesConfirmed)
        advanceTimeBy(150)
        runCurrent()

        assertTrue(contact.uiState.value.noticeLoadFailed)
        assertTrue(sleep.uiState.value.noticeLoadFailed)
        assertEquals(listOf("old"), sleep.uiState.value.notices.map { it.id })
        store.clear()
    }

    @Test
    fun `画面 ViewModel の破棄で消去処理もキャンセルし、失敗として表示しない`() = runTest {
        val fixture = NotificationTestFixture()
        fixture.noticeRepository.refreshNotices()
        val sleep = sleepViewModel(fixture)
        var cancelled = false
        fixture.noticeApi.deleteResponse = {
            try {
                awaitCancellation()
            } finally {
                cancelled = true
            }
        }
        sleep.onIntent(SleepIntent.ClearNoticesConfirmed)
        runCurrent()
        store.clear()
        runCurrent()

        assertTrue(cancelled)
        assertFalse(fixture.noticeRepository.noticeSnapshot.value.loadFailed)
        assertEquals(listOf("old"), fixture.noticeRepository.noticeSnapshot.value.notices.map { it.id })
        fixture.noticeRepository.refreshNotices()
        assertEquals(listOf("list", "delete", "list"), fixture.noticeApi.calls)
    }
}
