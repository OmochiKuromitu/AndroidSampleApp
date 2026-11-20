package com.example.androidsampleapp.ui.groupcall

import com.example.androidsampleapp.core.mvi.UiEffect

/** グループ通話の画面が出す一回きりの出来事。 */
sealed interface GroupCallEffect : UiEffect {
    /** 通話を終えた。どこへ戻るかは AppNavigation が決める。 */
    data object CallEnded : GroupCallEffect
}
