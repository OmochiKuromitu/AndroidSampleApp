package com.example.androidsampleapp.ui.aircon

import androidx.annotation.StringRes
import com.example.androidsampleapp.core.mvi.UiEffect

sealed interface AirconEffect : UiEffect {
    data class ShowMessage(@StringRes val messageRes: Int) : AirconEffect
}
