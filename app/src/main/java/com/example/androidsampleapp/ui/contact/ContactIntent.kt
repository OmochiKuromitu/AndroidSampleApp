package com.example.androidsampleapp.ui.contact

import com.example.androidsampleapp.core.NoticeSnapshot
import com.example.androidsampleapp.core.mvi.UiIntent
import com.example.androidsampleapp.domain.model.AddressBook
import com.example.androidsampleapp.domain.model.Notice

/**
 * 連絡先画面の状態を変えうる入力の一覧。
 *
 * 利用者の操作、ContactRepository の電話帳と履歴の変化、取得の失敗、MissedCallManager の件数と一覧の変化を、
 * すべてここから Reducer に通す。
 */
sealed interface ContactIntent : UiIntent {
    /** 画面の生成時に 1 度だけ流す。ここで電話帳と履歴を取り直す。 */
    data object Started : ContactIntent

    /** 画面の中のリスト切り替え。遷移ではないので Effect は出さない。 */
    data class ListSelected(val list: ContactList) : ContactIntent

    /**
     * 電話帳と履歴が変わった。ViewModel が ContactRepository を購読して、値が流れるたびに投げる。
     * 画面を開いた時点で前回の値があれば、取り直しを待たずにまずそれが流れる。
     */
    data class AddressBookChanged(val addressBook: AddressBook) : ContactIntent

    /** 電話帳か履歴のどちらかの取得に失敗した。片方だけ出すことはしない。 */
    data object LoadFailed : ContactIntent

    /** 不在着信の件数の変化。取得は MissedCallManager が行う。 */
    data class MissedCallCountChanged(val count: Int) : ContactIntent

    /** お知らせの一覧の変化。取得・消去の結果も、その失敗もここに戻る。取得は MissedCallManager が行う。 */
    data class NoticesChanged(val snapshot: NoticeSnapshot) : ContactIntent

    /** お知らせタブの消去ボタンを押した。まだ消さず、確認ダイアログを出す。 */
    data object ClearNoticesClicked : ContactIntent

    /** 確認ダイアログで「はい」を押した。スリープ画面の一覧も同じものが消える。 */
    data object ClearNoticesConfirmed : ContactIntent

    /** 確認ダイアログを閉じた（「いいえ」、または外側をタップ）。何も消さない。 */
    data object ClearNoticesDismissed : ContactIntent

    /** お知らせをタップした。飛び先への遷移は Effect で AppNavigation に伝える。 */
    data class NoticeClicked(val notice: Notice) : ContactIntent
}
