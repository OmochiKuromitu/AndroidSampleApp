package com.example.androidsampleapp.ui.contact

import com.example.androidsampleapp.core.NoticeSnapshot
import com.example.androidsampleapp.core.mvi.UiIntent
import com.example.androidsampleapp.domain.model.CallHistory
import com.example.androidsampleapp.domain.model.Contact
import com.example.androidsampleapp.domain.model.Notice

sealed interface ContactIntent : UiIntent {
    /** 画面の生成時に 1 度だけ流す。ここで電話帳と履歴を取りに行く。 */
    data object Started : ContactIntent

    /** 画面の中のリスト切り替え。遷移ではないので Effect は出さない。 */
    data class ListSelected(val list: ContactList) : ContactIntent

    data class Loaded(
        val contacts: List<Contact>,
        val histories: List<CallHistory>,
    ) : ContactIntent

    data object LoadFailed : ContactIntent

    /** 不在着信の件数の変化。取得は MissedCallManager が行う。 */
    data class MissedCallCountChanged(val count: Int) : ContactIntent

    /** お知らせの一覧の変化。取得は NoticeManager が行う。 */
    data class NoticesChanged(val snapshot: NoticeSnapshot) : ContactIntent

    data object ClearNoticesClicked : ContactIntent
    data class NoticeClicked(val notice: Notice) : ContactIntent
}
