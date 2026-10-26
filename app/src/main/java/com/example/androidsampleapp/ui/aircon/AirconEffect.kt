package com.example.androidsampleapp.ui.aircon

import androidx.annotation.StringRes
import com.example.androidsampleapp.core.mvi.UiEffect

/**
 * エアコン画面が出す一回きりの出来事。状態として持つと、画面を開き直したときにまた出てしまうものをここに置く。
 */
sealed interface AirconEffect : UiEffect {
    /** HTTP 成功時の業務エラー。サーバの res をそのまま画面に渡す。 */
    data class ShowErrorDialog(val resultCode: Int) : AirconEffect

    /**
     * スナックバーで知らせる（今はコマンドの送信失敗だけ）。
     * [text] があればそれを出す（サーバがエラー本文で返した message）。無ければ [messageRes] を出す。
     * 文言は画面側で解決する。ViewModel が Context を持たずに済む。
     */
    data class ShowMessage(@StringRes val messageRes: Int, val text: String? = null) : AirconEffect
}
