package com.example.androidsampleapp.ui.contact

import androidx.annotation.StringRes
import com.example.androidsampleapp.R
import com.example.androidsampleapp.core.mvi.UiState
import com.example.androidsampleapp.domain.model.CallHistory
import com.example.androidsampleapp.domain.model.Contact
import com.example.androidsampleapp.ui.common.Route

/**
 * 画面の中のリスト。下部バーのタブとは別物で、遷移は伴わない。
 * どちらを表示しているかは画面の状態なので [ContactState] が持つ。
 */
enum class ContactList(@StringRes val labelRes: Int) {
    PHONEBOOK(R.string.contact_phonebook),
    HISTORY(R.string.contact_history),
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

data class ContactState(
    val selectedList: ContactList = ContactList.PHONEBOOK,
    val contacts: List<Contact> = emptyList(),
    val histories: List<CallHistory> = emptyList(),
    val isLoading: Boolean = false,
    val loadFailed: Boolean = false,
    /** 履歴タブに出すバッジの件数。0 なら出さない。 */
    val missedCallCount: Int = 0,
) : UiState
