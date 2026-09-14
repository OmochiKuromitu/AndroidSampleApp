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

        is ContactIntent.MissedCallCountChanged -> state.copy(missedCallCount = intent.count)

        is ContactIntent.NoticesChanged -> state.copy(
            notices = intent.snapshot.notices,
            isLoadingNotices = intent.snapshot.isLoading,
            noticeLoadFailed = intent.snapshot.loadFailed,
        )

        // 取得済みの一覧を出し分けるだけなので、通信は起きない。
        is ContactIntent.ListSelected -> state.copy(selectedList = intent.list)

        // 読み込み中かどうかは NoticeManager が決めて NoticesChanged で戻る。先読みしない。
        ContactIntent.ClearNoticesClicked -> state

        // 遷移は状態変化ではないので Effect で扱う。
        is ContactIntent.NoticeClicked -> state
    }
}
