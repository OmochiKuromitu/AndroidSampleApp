package com.example.androidsampleapp.ui.contact

import androidx.annotation.StringRes
import com.example.androidsampleapp.R
import com.example.androidsampleapp.core.mvi.UiState
import com.example.androidsampleapp.domain.model.CallHistory
import com.example.androidsampleapp.domain.model.Contact
import com.example.androidsampleapp.domain.model.Notice
import com.example.androidsampleapp.ui.common.Route

/**
 * 画面の中のリスト。下部バーのタブとは別物で、遷移は伴わない。
 * どれを表示しているかは画面の状態なので [ContactState] が持つ。
 */
enum class ContactList(@StringRes val labelRes: Int) {
    PHONEBOOK(R.string.contact_phonebook),
    HISTORY(R.string.contact_history),
    NOTICE(R.string.contact_notice),
    ;

    companion object {
        /**
         * 遷移時に渡された引数から、最初に開くリストを決める。
         * 引数が無い、あるいは知らない値なら電話帳から開く。
         */
        fun fromArgument(value: String?): ContactList =
            if (value == Route.CONTACT_LIST_HISTORY) HISTORY else PHONEBOOK
    }
}

/**
 * 連絡先画面の状態。
 *
 * 電話帳と履歴は画面を開いたときにまとめて取り、ここに持つ（タブの切り替えでは取り直さない）。
 * 不在着信の件数とお知らせの一覧は複数の画面が見るので、持ち主はそれぞれ MissedCallManager と
 * NoticeManager で、ここにはその写しが入る。
 */
data class ContactState(
    /** 画面の中で今出しているリスト。 */
    val selectedList: ContactList = ContactList.PHONEBOOK,
    val contacts: List<Contact> = emptyList(),
    val histories: List<CallHistory> = emptyList(),
    /** 電話帳と履歴を取得している間。 */
    val isLoading: Boolean = false,
    /** 電話帳と履歴の取得に失敗した。 */
    val loadFailed: Boolean = false,
    /** 履歴タブに出すバッジの件数。0 なら出さない。 */
    val missedCallCount: Int = 0,
    /** お知らせタブに出す一覧。持ち主は NoticeManager で、ここにはその写しが入る。 */
    val notices: List<Notice> = emptyList(),
    /** NoticeManager がお知らせを取得（または消去して取り直し）している間。 */
    val isLoadingNotices: Boolean = false,
    /** NoticeManager の直近の取得に失敗した。前回の一覧は [notices] に残る。 */
    val noticeLoadFailed: Boolean = false,
) : UiState
