package com.example.androidsampleapp.ui.sleep

import com.example.androidsampleapp.core.mvi.UiState
import com.example.androidsampleapp.domain.model.Notice

data class SleepState(
    val timeText: String = "",
    val dateText: String = "",
    /** API から取った通知。保持するのはここで、リポジトリは持たない。 */
    val apiNotices: List<Notice> = emptyList(),
    /** 機器から届いた通知。持ち主は AppStateHolder で、ここにはその写しが入る。 */
    val deviceNotices: List<Notice> = emptyList(),
    val isLoadingNotices: Boolean = false,
    val noticeLoadFailed: Boolean = false,
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

    val isUnlockReached: Boolean
        get() = unlockProgress >= 1f
}
