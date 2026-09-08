package com.example.androidsampleapp.ui.top

import androidx.annotation.StringRes
import com.example.androidsampleapp.core.mvi.UiEffect

sealed interface TopEffect : UiEffect {
    /** 文言は画面側で解決する。ViewModel が Context を持たずに済む。 */
    data class ShowMessage(@StringRes val messageRes: Int) : TopEffect
}
