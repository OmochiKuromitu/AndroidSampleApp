package com.example.androidsampleapp.ui.sleep

import com.example.androidsampleapp.core.mvi.UiEffect

sealed interface SleepEffect : UiEffect {
    /** 復帰。実際の遷移は ui/navigation 側が行う。 */
    data object Wake : SleepEffect
}
