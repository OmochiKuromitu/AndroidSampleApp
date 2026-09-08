package com.example.androidsampleapp.ui.main

import com.example.androidsampleapp.core.mvi.UiEffect

/**
 * 遷移は一回きりの命令なので Effect にする。
 * State に持たせると、画面回転などの再生成のたびに再遷移してしまう。
 */
sealed interface MainEffect : UiEffect {
    data class NavigateToTab(val tab: MainTab) : MainEffect
    data class PopToTabRoot(val tab: MainTab) : MainEffect
    data object NavigateToSleep : MainEffect
}
