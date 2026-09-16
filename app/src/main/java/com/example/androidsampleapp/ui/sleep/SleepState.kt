package com.example.androidsampleapp.ui.sleep

import com.example.androidsampleapp.core.mvi.UiState
import com.example.androidsampleapp.domain.model.Notice

/**
 * スリープ画面の状態。時計、通知の一覧、解除スワイプの進み具合を持つ。
 *
 * スリープに入るたびに ViewModel ごと作り直されるので、ここに持つ値もそのたびに初期値から始まる。
 * API の通知はリポジトリが前回の値を持っているので、作り直しても取り直しを待たずにすぐ入る。
 */
data class SleepState(
    /** 表示用に整形済みの時刻（「21:47」）。整形は ViewModel が行う。 */
    val timeText: String = "",
    /** 表示用に整形済みの日付（「9月10日 (水)」）。 */
    val dateText: String = "",
    /** API の通知。持ち主は NoticeRepository で、ここにはその写しが入る。 */
    val apiNotices: List<Notice> = emptyList(),
    /**
     * API の通知を一度でも受け取れたか。
     * 「まだ取っていない空」と「取ったら空だった」を分けるために持つ。
     */
    val isApiNoticesLoaded: Boolean = false,
    /** 機器から届いた通知。持ち主は AppStateHolder で、ここにはその写しが入る。 */
    val deviceNotices: List<Notice> = emptyList(),
    /** 直近の API の取得（または消去）に失敗した。受け取り済みの一覧があればそれを出したままにする。 */
    val noticeLoadFailed: Boolean = false,
    /** 全消去の確認ダイアログを出しているか。誤操作で消さないよう、1 度だけ確かめる。 */
    val isClearConfirmVisible: Boolean = false,
    /**
     * 解除スワイプの進み具合。0f = 触っていない、1f = 解除に必要な距離に到達。
     * 指の動きに合わせて手応えを返すために状態として持つ。
     */
    val unlockProgress: Float = 0f,
) : UiState {
    /**
     * 一覧に出す通知。出どころ別に持っておき、ここで合わせて新しい順に並べる。
     * 分けて持つのは、API を取り直したときに機器からの分を消さないため（逆も同じ）。
     *
     * 描画のたびに並べ直さないよう、インスタンスを作ったときに 1 度だけ計算する。
     */
    val notices: List<Notice> = (apiNotices + deviceNotices).sortedByDescending { it.occurredAt }

    /**
     * API の通知をまだ一度も受け取れておらず、失敗もしていない間。
     *
     * 取得の完了を Intent で待たずに、受け取れたかどうかから決める。StateFlow は同じ値を
     * 入れ直しても流れないので、「流れてきたら読み込み終わり」にすると取り直しで止まるため。
     */
    val isLoadingNotices: Boolean
        get() = !isApiNoticesLoaded && !noticeLoadFailed

    /** 解除に必要な距離に届いたか。届いた瞬間に ViewModel が解除の Effect を出す。 */
    val isUnlockReached: Boolean
        get() = unlockProgress >= 1f
}
