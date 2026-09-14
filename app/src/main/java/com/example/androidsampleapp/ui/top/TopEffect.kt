package com.example.androidsampleapp.ui.top

import androidx.annotation.StringRes
import com.example.androidsampleapp.core.mvi.UiEffect

/**
 * トップ画面が出す一回きりの出来事。状態として持つと、画面を開き直したときにまた出てしまうものをここに置く。
 */
sealed interface TopEffect : UiEffect {
    /**
     * スナックバーで知らせる（応答した、拒否した、送れなかった）。
     * 文言は画面側で解決する。ViewModel が Context を持たずに済む。
     */
    data class ShowMessage(@StringRes val messageRes: Int) : TopEffect
}
