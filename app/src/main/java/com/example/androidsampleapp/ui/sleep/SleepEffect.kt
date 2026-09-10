package com.example.androidsampleapp.ui.sleep

import com.example.androidsampleapp.core.mvi.UiEffect
import com.example.androidsampleapp.domain.model.NoticeDestination

sealed interface SleepEffect : UiEffect {
    /** 復帰。実際の遷移は ui/navigation 側が行う。 */
    data object Wake : SleepEffect

    /** 復帰したうえで、通知が指す画面を開く。 */
    data class OpenDestination(val destination: NoticeDestination) : SleepEffect
}
