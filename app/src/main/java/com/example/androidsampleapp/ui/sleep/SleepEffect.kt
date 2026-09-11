package com.example.androidsampleapp.ui.sleep

import com.example.androidsampleapp.core.mvi.UiEffect
import com.example.androidsampleapp.domain.model.NoticeDestination

/**
 * 「何が起きたか」を伝えるだけ。どこへ行くかは AppNavigation が決める。
 */
sealed interface SleepEffect : UiEffect {
    /** 解除の操作が成立した。 */
    data object Unlocked : SleepEffect

    /** 通知が選ばれた。その通知が指す飛び先を添える。 */
    data class NoticeSelected(val destination: NoticeDestination) : SleepEffect
}
