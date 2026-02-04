package com.example.androidsampleapp.ui.sleep

import com.example.androidsampleapp.core.mvi.UiState
import com.example.androidsampleapp.domain.model.Notice

/**
 * スリープ画面の状態。時計、通知の一覧、解除スワイプの進み具合を持つ。
 *
 * スリープに入るたびに ViewModel ごと作り直されるので、ここに持つ値もそのたびに初期値から始まる。
 * 通知の一覧は MissedCallManager の写しなので、作り直しても取得済みの分がすぐ入る。
 */
data class SleepState(
    /** 表示用に整形済みの時刻（「21:47」）。整形は ViewModel が行う。 */
    val timeText: String = "",
    /** 表示用に整形済みの日付（「9月10日 (水)」）。 */
    val dateText: String = "",
    /** 一覧に出す通知。持ち主は MissedCallManager で、ここにはその写しが入る。 */
    val notices: List<Notice> = emptyList(),
    /** API の通知をまだ一度も受け取れておらず、失敗もしていない間。決めるのは MissedCallManager。 */
    val isLoadingNotices: Boolean = false,
    /** MissedCallManager の直近の取得（または消去）に失敗した。受け取り済みの一覧は [notices] に残る。 */
    val noticeLoadFailed: Boolean = false,
    /** 全消去の確認ダイアログを出しているか。誤操作で消さないよう、1 度だけ確かめる。 */
    val isClearConfirmVisible: Boolean = false,
    /**
     * 解除スワイプの進み具合。0f = 触っていない、1f = 解除に必要な距離に到達。
     * 指の動きに合わせて手応えを返すために状態として持つ。
     */
    val unlockProgress: Float = 0f,
) : UiState {
    /** 解除に必要な距離に届いたか。届いた瞬間に ViewModel が解除の Effect を出す。 */
    val isUnlockReached: Boolean
        get() = unlockProgress >= 1f
}
