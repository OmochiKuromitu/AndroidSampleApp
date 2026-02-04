package com.example.androidsampleapp.ui.contact

import com.example.androidsampleapp.core.NoticeSnapshot
import com.example.androidsampleapp.domain.model.AddressBook
import com.example.androidsampleapp.domain.model.CallHistory
import com.example.androidsampleapp.domain.model.Contact
import com.example.androidsampleapp.domain.model.Notice
import com.example.androidsampleapp.domain.model.NoticeCategory
import com.example.androidsampleapp.domain.model.NoticeDestination
import com.example.androidsampleapp.ui.common.Route
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactReducerTest {

    private val reducer = ContactReducer()

    private val contacts = listOf(Contact("1", "管理室", "0001"))
    private val histories = listOf(CallHistory("1", "玄関", "9月11日 14:32", isMissed = true))
    private val notices = listOf(
        Notice("1", NoticeCategory.INFO, null, "本文", 100L, NoticeDestination.Top),
    )

    @Test
    fun `初めは電話帳と履歴を受け取るまで読み込み中`() {
        assertTrue(ContactState().isLoading)
    }

    @Test
    fun `Started で前回の失敗が消えて読み込み中に戻る`() {
        val next = reducer.reduce(ContactState(loadFailed = true), ContactIntent.Started)

        assertTrue(next.isLoading)
        assertFalse(next.loadFailed)
    }

    @Test
    fun `受け取り済みなら Started でも読み込み中にしない`() {
        // 開き直したときは前回の一覧を出したまま取り直す。
        val state = ContactState(contacts = contacts, histories = histories, isAddressBookLoaded = true)

        val next = reducer.reduce(state, ContactIntent.Started)

        assertFalse(next.isLoading)
        assertEquals(contacts, next.contacts)
    }

    @Test
    fun `電話帳と履歴を受け取ったら両方の一覧が入る`() {
        val next = reducer.reduce(
            ContactState(),
            ContactIntent.AddressBookChanged(AddressBook(contacts = contacts, histories = histories)),
        )

        assertEquals(contacts, next.contacts)
        assertEquals(histories, next.histories)
        assertFalse(next.isLoading)
    }

    @Test
    fun `取ったら空だった場合も読み込み中が解ける`() {
        val next = reducer.reduce(
            ContactState(),
            ContactIntent.AddressBookChanged(AddressBook(contacts = emptyList(), histories = emptyList())),
        )

        assertFalse(next.isLoading)
    }

    @Test
    fun `取得に失敗したら失敗が立つ`() {
        val next = reducer.reduce(ContactState(), ContactIntent.LoadFailed)

        assertFalse(next.isLoading)
        assertTrue(next.loadFailed)
    }

    @Test
    fun `取得に失敗しても受け取り済みの一覧は残す`() {
        val state = ContactState(contacts = contacts, histories = histories, isAddressBookLoaded = true)

        val next = reducer.reduce(state, ContactIntent.LoadFailed)

        assertEquals(contacts, next.contacts)
        assertTrue(next.loadFailed)
    }

    @Test
    fun `リストを切り替えても一覧は保持したまま`() {
        // 切り替えで通信し直さないので、取得済みの一覧が消えてはいけない。
        val state = ContactState(contacts = contacts, histories = histories)

        val next = reducer.reduce(state, ContactIntent.ListSelected(ContactList.HISTORY))

        assertEquals(ContactList.HISTORY, next.selectedList)
        assertEquals(contacts, next.contacts)
        assertEquals(histories, next.histories)
    }

    @Test
    fun `不在着信の件数を取り込む`() {
        val next = reducer.reduce(ContactState(), ContactIntent.MissedCallCountChanged(2))

        assertEquals(2, next.missedCallCount)
    }

    @Test
    fun `リストの切り替えでは件数を先読みしない`() {
        // 既読にするのは ViewModel の副作用側で、結果は MissedCallCountChanged で戻る。
        // Reducer が先に 0 にすると、既読 API が失敗したときに件数が消えたままになる。
        val state = ContactState(missedCallCount = 2)

        val next = reducer.reduce(state, ContactIntent.ListSelected(ContactList.HISTORY))

        assertEquals(2, next.missedCallCount)
    }

    @Test
    fun `お知らせの一覧と読み込み状態を取り込む`() {
        val next = reducer.reduce(
            ContactState(isLoadingNotices = true),
            ContactIntent.NoticesChanged(NoticeSnapshot(notices = notices, isLoaded = true)),
        )

        assertEquals(notices, next.notices)
        assertFalse(next.isLoadingNotices)
        assertFalse(next.noticeLoadFailed)
    }

    @Test
    fun `お知らせをまだ受け取っていなければお知らせだけ読み込み中になる`() {
        val state = ContactState(contacts = contacts, histories = histories, isAddressBookLoaded = true)

        val next = reducer.reduce(state, ContactIntent.NoticesChanged(NoticeSnapshot()))

        assertTrue(next.isLoadingNotices)
        assertFalse(next.isLoading)
    }

    @Test
    fun `お知らせの変化は電話帳と履歴の読み込み状態に触らない`() {
        val next = reducer.reduce(
            ContactState(),
            ContactIntent.NoticesChanged(NoticeSnapshot(loadFailed = true)),
        )

        assertTrue(next.noticeLoadFailed)
        assertTrue(next.isLoading)
        assertFalse(next.loadFailed)
    }

    @Test
    fun `お知らせの消去を押した時点では確認ダイアログを出すだけで消さない`() {
        val state = ContactState(notices = notices)

        val next = reducer.reduce(state, ContactIntent.ClearNoticesClicked)

        assertEquals(state.copy(isClearConfirmVisible = true), next)
    }

    @Test
    fun `お知らせの消去ではいを押したらダイアログを閉じる。読み込み中は先読みしない`() {
        val state = ContactState(notices = notices, isClearConfirmVisible = true)

        val next = reducer.reduce(state, ContactIntent.ClearNoticesConfirmed)

        assertEquals(state.copy(isClearConfirmVisible = false), next)
    }

    @Test
    fun `お知らせの消去をいいえで閉じたら何も起きない`() {
        val state = ContactState(notices = notices, isClearConfirmVisible = true)

        val next = reducer.reduce(state, ContactIntent.ClearNoticesDismissed)

        assertEquals(state.copy(isClearConfirmVisible = false), next)
    }

    @Test
    fun `お知らせのタップは状態を変えない`() {
        val state = ContactState(notices = notices)

        val next = reducer.reduce(state, ContactIntent.NoticeClicked(notices.first()))

        assertEquals(state, next)
    }

    @Test
    fun `引数が履歴なら履歴から開く`() {
        assertEquals(
            ContactList.HISTORY,
            ContactList.fromArgument(Route.CONTACT_LIST_HISTORY),
        )
    }

    @Test
    fun `引数が無いか知らない値なら電話帳から開く`() {
        assertEquals(ContactList.PHONEBOOK, ContactList.fromArgument(null))
        assertEquals(ContactList.PHONEBOOK, ContactList.fromArgument("unknown"))
    }
}
