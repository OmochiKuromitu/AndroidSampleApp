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
 * 電話帳と履歴の持ち主は ContactRepository で、ここにはその写しが入る。
 * 取り直すのは画面を開いたときだけで、タブの切り替えでは取り直さない。
 * 不在着信の件数とお知らせの一覧は複数の画面が見るので、持ち主はどちらも MissedCallManager で、
 * ここにはその写しが入る。
 */
data class ContactState(
    /** 画面の中で今出しているリスト。 */
    val selectedList: ContactList = ContactList.PHONEBOOK,
    val contacts: List<Contact> = emptyList(),
    val histories: List<CallHistory> = emptyList(),
    /**
     * 電話帳と履歴を一度でも受け取れたか。
     * 「まだ取っていない空」と「取ったら空だった」を分けるために持つ。
     */
    val isAddressBookLoaded: Boolean = false,
    /** 直近の取得に失敗した。受け取り済みの一覧があればそれを出したままにする。 */
    val loadFailed: Boolean = false,
    /** 履歴タブに出すバッジの件数。0 なら出さない。 */
    val missedCallCount: Int = 0,
    /** お知らせタブに出す一覧。持ち主は MissedCallManager で、ここにはその写しが入る。 */
    val notices: List<Notice> = emptyList(),
    /** お知らせをまだ一度も受け取れておらず、失敗もしていない間。決めるのは MissedCallManager。 */
    val isLoadingNotices: Boolean = false,
    /** MissedCallManager の直近の取得（または消去）に失敗した。受け取り済みの一覧は [notices] に残る。 */
    val noticeLoadFailed: Boolean = false,
    /** お知らせの全消去の確認ダイアログを出しているか。スリープ画面と同じく、1 度だけ確かめる。 */
    val isClearConfirmVisible: Boolean = false,
) : UiState {
    /**
     * 電話帳と履歴をまだ一度も受け取れておらず、失敗もしていない間。
     *
     * 取得の完了を Intent で待たずに、受け取れたかどうかから決める。StateFlow は同じ値を
     * 入れ直しても流れないので、「流れてきたら読み込み終わり」にすると取り直しで止まるため。
     */
    val isLoading: Boolean
        get() = !isAddressBookLoaded && !loadFailed
}
