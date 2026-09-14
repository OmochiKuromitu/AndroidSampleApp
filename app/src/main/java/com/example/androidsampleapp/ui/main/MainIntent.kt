package com.example.androidsampleapp.ui.main

import com.example.androidsampleapp.core.mvi.UiIntent
import com.example.androidsampleapp.domain.model.ConnectionState

/**
 * メイン画面の枠（ヘッダーと下部バー）の状態を変えうる入力の一覧。
 *
 * 枠には利用者の操作から状態を変えるものが無い（タブのタップは遷移なので AppNavigation が扱う）。
 * ここに並ぶのは、共有状態の変化を Reducer に通すためのものだけ。
 */
sealed interface MainIntent : UiIntent {
    /** 機器との接続状態が変わった。ヘッダーの表示に使う。 */
    data class ConnectionStateChanged(val state: ConnectionState) : MainIntent

    /** 不在着信の件数が変わった。下部バーの連絡先タブのバッジに使う。取得は MissedCallManager が行う。 */
    data class MissedCallCountChanged(val count: Int) : MainIntent
}
