package com.example.androidsampleapp.ui.sleep

import com.example.androidsampleapp.core.mvi.UiState
import com.example.androidsampleapp.domain.model.Notice

/**
 * スリープ画面の状態。時計、通知の一覧、解除スワイプの進み具合を持つ。
 *
 * スリープに入るたびに ViewModel ごと作り直されるので、ここに持つ値もそのたびに初期値から始まる。
 * 通知の一覧は NoticeManager の写しなので、作り直しても取得済みの分がすぐ入る。
 */
data class SleepState(
    /** 表示用に整形済みの時刻（「21:47」）。整形は ViewModel が行う。 */
    val timeText: String = "",
    /** 表示用に整形済みの日付（「9月10日 (水)」）。 */
    val dateText: String = "",
    /** 一覧に出す通知。持ち主は NoticeManager で、ここにはその写しが入る。 */
    val notices: List<Notice> = emptyList(),
    /** NoticeManager が API から取得（または消去して取り直し）している間。 */
    val isLoadingNotices: Boolean = false,
    /** NoticeManager の直近の取得に失敗した。前回の一覧は [notices] に残る。 */
    val noticeLoadFailed: Boolean = false,
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
