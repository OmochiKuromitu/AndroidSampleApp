package com.example.androidsampleapp.ui.contact

import com.example.androidsampleapp.core.mvi.Reducer

/**
 * 連絡先画面の (状態, Intent) -> 次の状態。
 *
 * 純粋関数に保つ。取得と既読の呼び出しは ContactViewModel.handle が行う。
 */
class ContactReducer : Reducer<ContactState, ContactIntent> {
    override fun reduce(state: ContactState, intent: ContactIntent): ContactState = when (intent) {
        // 取り直しを始めるので、前回の失敗は消す。読み込み中かどうかは受け取れたかで決まる。
        ContactIntent.Started -> state.copy(loadFailed = false)

        is ContactIntent.AddressBookChanged -> state.copy(
            contacts = intent.addressBook.contacts,
            histories = intent.addressBook.histories,
            isAddressBookLoaded = true,
            loadFailed = false,
        )

        ContactIntent.LoadFailed -> state.copy(loadFailed = true)

        is ContactIntent.MissedCallCountChanged -> state.copy(missedCallCount = intent.count)

        // 取得済みの一覧を出し分けるだけなので、通信は起きない。
        is ContactIntent.ListSelected -> state.copy(selectedList = intent.list)
    }
}
