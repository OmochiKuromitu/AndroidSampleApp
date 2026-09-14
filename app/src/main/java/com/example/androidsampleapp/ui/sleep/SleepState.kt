package com.example.androidsampleapp.ui.sleep

import com.example.androidsampleapp.core.mvi.UiState
import com.example.androidsampleapp.domain.model.Notice

data class SleepState(
    val timeText: String = "",
    val dateText: String = "",
    /** 一覧に出す通知。持ち主は NoticeManager で、ここにはその写しが入る。 */
    val notices: List<Notice> = emptyList(),
    val isLoadingNotices: Boolean = false,
    val noticeLoadFailed: Boolean = false,
    /**
     * 解除スワイプの進み具合。0f = 触っていない、1f = 解除に必要な距離に到達。
     * 指の動きに合わせて手応えを返すために状態として持つ。
     */
    val unlockProgress: Float = 0f,
) : UiState {
    val isUnlockReached: Boolean
        get() = unlockProgress >= 1f
}
