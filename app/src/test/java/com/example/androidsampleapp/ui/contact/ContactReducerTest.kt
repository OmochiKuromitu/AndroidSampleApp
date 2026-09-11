package com.example.androidsampleapp.ui.contact

import com.example.androidsampleapp.domain.model.CallHistory
import com.example.androidsampleapp.domain.model.Contact
import com.example.androidsampleapp.ui.common.Route
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactReducerTest {

    private val reducer = ContactReducer()

    private val contacts = listOf(Contact("1", "管理室", "0001"))
    private val histories = listOf(CallHistory("1", "玄関", "9月11日 14:32", isMissed = true))

    @Test
    fun `Started で取得中になる`() {
        val next = reducer.reduce(ContactState(loadFailed = true), ContactIntent.Started)

        assertTrue(next.isLoading)
        assertFalse(next.loadFailed)
    }

    @Test
    fun `取得できた両方の一覧が入る`() {
        val next = reducer.reduce(
            ContactState(isLoading = true),
            ContactIntent.Loaded(contacts = contacts, histories = histories),
        )

        assertEquals(contacts, next.contacts)
        assertEquals(histories, next.histories)
        assertFalse(next.isLoading)
    }

    @Test
    fun `取得に失敗したら失敗が立つ`() {
        val next = reducer.reduce(ContactState(isLoading = true), ContactIntent.LoadFailed)

        assertFalse(next.isLoading)
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
    fun `リストを切り替えてもバッジの件数は変わらない`() {
        // 履歴を見ただけでは既読にしない。既読の API ができたらここが変わる。
        val state = ContactState(missedCallCount = 2)

        val next = reducer.reduce(state, ContactIntent.ListSelected(ContactList.HISTORY))

        assertEquals(2, next.missedCallCount)
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
