package com.example.androidsampleapp.ui.contact

import com.example.androidsampleapp.core.mvi.UiIntent
import com.example.androidsampleapp.domain.model.CallHistory
import com.example.androidsampleapp.domain.model.Contact

/**
 * 連絡先画面の状態を変えうる入力の一覧。
 *
 * 利用者の操作、取得の結果、MissedCallManager の件数の変化を、すべてここから Reducer に通す。
 */
sealed interface ContactIntent : UiIntent {
    /** 画面の生成時に 1 度だけ流す。ここで両方のリストを取りに行く。 */
    data object Started : ContactIntent

    /** 画面の中のリスト切り替え。遷移ではないので Effect は出さない。 */
    data class ListSelected(val list: ContactList) : ContactIntent

    /** 電話帳と履歴の両方を取得できた。 */
    data class Loaded(
        val contacts: List<Contact>,
        val histories: List<CallHistory>,
    ) : ContactIntent

    /** 電話帳か履歴のどちらかの取得に失敗した。片方だけ出すことはしない。 */
    data object LoadFailed : ContactIntent

    /** 不在着信の件数の変化。取得は MissedCallManager が行う。 */
    data class MissedCallCountChanged(val count: Int) : ContactIntent
}
