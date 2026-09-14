package com.example.androidsampleapp.ui.sleep

import com.example.androidsampleapp.core.NoticeSnapshot
import com.example.androidsampleapp.core.mvi.UiIntent
import com.example.androidsampleapp.domain.model.Notice

/**
 * スリープ画面の状態を変えうる入力の一覧。
 *
 * 時計の更新、NoticeManager の一覧の変化、利用者の操作（消去、通知のタップ、解除スワイプ）を、
 * すべてここから Reducer に通す。
 */
sealed interface SleepIntent : UiIntent {
    /** 時計の更新。ViewModel が 1 秒ごとに、整形済みの文字列で投げる。 */
    data class Ticked(val timeText: String, val dateText: String) : SleepIntent

    /** NoticeManager の一覧の変化。取得・消去の結果も、機器から届いた通知もここに戻る。 */
    data class NoticesChanged(val snapshot: NoticeSnapshot) : SleepIntent

    /** 消去ボタンを押した。API の通知と機器から届いた通知の両方を消して取り直す。 */
    data object ClearNoticesClicked : SleepIntent

    /** 通知をタップした。飛び先への遷移は Effect で AppNavigation に伝える。 */
    data class NoticeClicked(val notice: Notice) : SleepIntent

    /** 下端から上へスワイプ中。[progress] は 0f..1f に正規化済み。 */
    data class UnlockDragged(val progress: Float) : SleepIntent

    /** 解除に届かないまま指が離れた、あるいはジェスチャが中断された。 */
    data object UnlockCancelled : SleepIntent
}
