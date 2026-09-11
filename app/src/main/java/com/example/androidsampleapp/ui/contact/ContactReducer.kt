package com.example.androidsampleapp.ui.contact

import com.example.androidsampleapp.core.mvi.Reducer

class ContactReducer : Reducer<ContactState, ContactIntent> {
    override fun reduce(state: ContactState, intent: ContactIntent): ContactState = when (intent) {
        ContactIntent.Started -> state.copy(isLoading = true, loadFailed = false)

        is ContactIntent.Loaded -> state.copy(
            contacts = intent.contacts,
            histories = intent.histories,
            isLoading = false,
            loadFailed = false,
        )

        ContactIntent.LoadFailed -> state.copy(isLoading = false, loadFailed = true)

        // 取得済みの一覧を出し分けるだけなので、通信は起きない。
        is ContactIntent.ListSelected -> state.copy(selectedList = intent.list)
    }
}
