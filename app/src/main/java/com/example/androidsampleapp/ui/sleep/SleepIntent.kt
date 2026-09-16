package com.example.androidsampleapp.ui.sleep

import com.example.androidsampleapp.core.mvi.UiIntent
import com.example.androidsampleapp.domain.model.Notice

/**
 * スリープ画面の状態を変えうる入力の一覧。
 *
 * 時計の更新、NoticeRepository の通知の変化、取得の失敗、機器から届く通知、解除スワイプまで、
 * すべてここから Reducer に通す。
 */
sealed interface SleepIntent : UiIntent {
    /** 画面の生成時に 1 度だけ流す。ここで API の通知を取り直す。 */
    data object Started : SleepIntent

    /** 時計の更新。ViewModel が 1 秒ごとに、整形済みの文字列で投げる。 */
    data class Ticked(val timeText: String, val dateText: String) : SleepIntent

    /**
     * API の通知が変わった。ViewModel が NoticeRepository を購読して、値が流れるたびに投げる。
     * 取り直しや消去の結果もここに戻る。前回の値があれば、取り直しを待たずにまずそれが流れる。
     */
    data class ApiNoticesChanged(val notices: List<Notice>) : SleepIntent

    /** 取得（または消去）に失敗した。前回の一覧は残す。 */
    data object NoticesLoadFailed : SleepIntent

    /** 機器から届いた通知の変化。スリープ中でも届くたびに流れる。 */
    data class DeviceNoticesChanged(val notices: List<Notice>) : SleepIntent

    /** 消去ボタンを押した。API の通知と機器から届いた通知の両方を消して取り直す。 */
    data object ClearNoticesClicked : SleepIntent

    /** 通知をタップした。飛び先への遷移は Effect で AppNavigation に伝える。 */
    data class NoticeClicked(val notice: Notice) : SleepIntent

    /** 下端から上へスワイプ中。[progress] は 0f..1f に正規化済み。 */
    data class UnlockDragged(val progress: Float) : SleepIntent

    /** 解除に届かないまま指が離れた、あるいはジェスチャが中断された。 */
    data object UnlockCancelled : SleepIntent
}
