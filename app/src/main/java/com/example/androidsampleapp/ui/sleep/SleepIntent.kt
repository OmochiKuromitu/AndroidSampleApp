package com.example.androidsampleapp.ui.sleep

import com.example.androidsampleapp.core.NoticeSnapshot
import com.example.androidsampleapp.core.mvi.UiIntent
import com.example.androidsampleapp.domain.model.Notice

/**
 * スリープ画面の状態を変えうる入力の一覧。
 *
 * 時計の更新、MissedCallManager の一覧の変化、利用者の操作（消去、通知のタップ、解除スワイプ）を、
 * すべてここから Reducer に通す。
 */
sealed interface SleepIntent : UiIntent {
    /** 時計の更新。ViewModel が 1 秒ごとに、整形済みの文字列で投げる。 */
    data class Ticked(val timeText: String, val dateText: String) : SleepIntent

    /**
     * MissedCallManager の一覧の変化。取得・消去の結果も、その失敗もここに戻る。
     * 前回の値があれば、取り直しを待たずにまずそれが流れる。
     */
    data class NoticesChanged(val snapshot: NoticeSnapshot) : SleepIntent

    /** 消去ボタンを押した。まだ消さず、確認ダイアログを出す。 */
    data object ClearNoticesClicked : SleepIntent

    /** 確認ダイアログで「はい」を押した。通知を消して取り直す。 */
    data object ClearNoticesConfirmed : SleepIntent

    /** 確認ダイアログを閉じた（「いいえ」、または外側をタップ）。何も消さない。 */
    data object ClearNoticesDismissed : SleepIntent

    /** 通知をタップした。飛び先への遷移は Effect で AppNavigation に伝える。 */
    data class NoticeClicked(val notice: Notice) : SleepIntent

    /** 下端から上へスワイプ中。[progress] は 0f..1f に正規化済み。 */
    data class UnlockDragged(val progress: Float) : SleepIntent

    /** 解除に届かないまま指が離れた、あるいはジェスチャが中断された。 */
    data object UnlockCancelled : SleepIntent
}
