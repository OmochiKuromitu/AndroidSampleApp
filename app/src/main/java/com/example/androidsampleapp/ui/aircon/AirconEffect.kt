package com.example.androidsampleapp.ui.aircon

import androidx.annotation.StringRes
import com.example.androidsampleapp.core.mvi.UiEffect

/**
 * エアコン画面が出す一回きりの出来事。状態として持つと、画面を開き直したときにまた出てしまうものをここに置く。
 */
sealed interface AirconEffect : UiEffect {
    /**
     * スナックバーで知らせる（今はコマンドの送信失敗だけ）。
     * 文言は画面側で解決する。ViewModel が Context を持たずに済む。
     */
    data class ShowMessage(@StringRes val messageRes: Int) : AirconEffect
}
