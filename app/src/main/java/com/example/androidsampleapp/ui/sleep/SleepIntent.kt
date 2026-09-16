package com.example.androidsampleapp.ui.sleep

import com.example.androidsampleapp.core.mvi.UiIntent
import com.example.androidsampleapp.domain.model.Notice

/**
 * スリープ画面の状態を変えうる入力の一覧。
 *
 * 時計の更新、通知の取得結果、機器から届く通知、解除スワイプまで、すべてここから Reducer に通す。
 */
sealed interface SleepIntent : UiIntent {
    /** 画面の生成時に 1 度だけ流す。ここで通知を取りに行く。 */
    data object Started : SleepIntent

    /** 時計の更新。ViewModel が 1 秒ごとに、整形済みの文字列で投げる。 */
    data class Ticked(val timeText: String, val dateText: String) : SleepIntent

    /** 取得の結果。消去の結果もここに戻る（消去は取り直しを伴うため）。 */
    data class NoticesLoaded(val notices: List<Notice>) : SleepIntent
    /** 取得（または消去）に失敗した。前回の一覧は残す。 */
    data object NoticesLoadFailed : SleepIntent

    /** 機器から届いた通知の変化。スリープ中でも届くたびに流れる。 */
    data class DeviceNoticesChanged(val notices: List<Notice>) : SleepIntent

    /** 消去ボタンを押した。まだ消さず、確認ダイアログを出す。 */
    data object ClearNoticesClicked : SleepIntent

    /** 確認ダイアログで「はい」を押した。API の通知と機器から届いた通知の両方を消して取り直す。 */
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
