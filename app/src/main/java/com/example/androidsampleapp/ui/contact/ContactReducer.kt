package com.example.androidsampleapp.ui.contact

import com.example.androidsampleapp.core.mvi.Reducer

/**
 * 連絡先画面の (状態, Intent) -> 次の状態。
 *
 * 純粋関数に保つ。取得、既読、お知らせの消去の呼び出しは ContactViewModel.handle が行う。
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

        is ContactIntent.NoticesChanged -> state.copy(
            notices = intent.snapshot.notices,
            isLoadingNotices = intent.snapshot.isLoading,
            noticeLoadFailed = intent.snapshot.loadFailed,
        )

        // 取得済みの一覧を出し分けるだけなので、通信は起きない。
        is ContactIntent.ListSelected -> state.copy(selectedList = intent.list)

        // 押しただけでは消さない。確認してから。
        ContactIntent.ClearNoticesClicked -> state.copy(isClearConfirmVisible = true)

        // 読み込み中かどうかは MissedCallManager が決めて NoticesChanged で戻る。先読みしない。
        ContactIntent.ClearNoticesConfirmed,
        ContactIntent.ClearNoticesDismissed,
        -> state.copy(isClearConfirmVisible = false)

        // 遷移は状態変化ではないので Effect で扱う。
        is ContactIntent.NoticeClicked -> state
    }
}
