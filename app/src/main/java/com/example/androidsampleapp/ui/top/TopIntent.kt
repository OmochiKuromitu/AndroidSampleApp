package com.example.androidsampleapp.ui.top

import com.example.androidsampleapp.core.mvi.UiIntent
import com.example.androidsampleapp.domain.model.Aircon
import com.example.androidsampleapp.domain.model.IncomingCall

/**
 * トップ画面の状態を変えうる入力の一覧。
 *
 * 共有状態の変化も Intent として受け取り、Reducer を唯一の入口に保つ。
 */
sealed interface TopIntent : UiIntent {
    // ---- 機器から降ってくる状態の変化（ViewModel が購読して投げる）

    /** 着信が始まった、または終わった（終わったら null）。 */
    data class IncomingCallChanged(val call: IncomingCall?) : TopIntent

    /** エアコンの現在値が変わった。要約カードに使う。 */
    data class AirconChanged(val aircon: Aircon) : TopIntent

    // ---- 利用者の操作

    /** 着信カードの応答ボタンを押した。 */
    data object AnswerClicked : TopIntent

    /** 着信カードの拒否ボタンを押した。 */
    data object RejectClicked : TopIntent

    // ---- コマンド送信の結果（ViewModel が送信後に投げる）

    /** 応答・拒否のコマンドを送り終えた。 */
    data object CommandSucceeded : TopIntent

    /** 送れなかった。押した時点で着信が消えていた場合もこれになる。 */
    data object CommandFailed : TopIntent
}
