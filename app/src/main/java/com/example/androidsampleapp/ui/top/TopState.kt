package com.example.androidsampleapp.ui.top

import com.example.androidsampleapp.config.BuildInfo
import com.example.androidsampleapp.core.mvi.UiState
import com.example.androidsampleapp.domain.model.Aircon
import com.example.androidsampleapp.domain.model.IncomingCall

/**
 * トップ画面の状態。着信とエアコンの要約を出す。
 *
 * 着信とエアコンの値は機器から降ってくる共有状態（AppStateHolder）の写しで、
 * 画面都合の値は送信中フラグだけ。
 * ビルドの情報は起動中に変わらないので、ViewModel を作るときの初期値で入れる。
 */
data class TopState(
    /** 着信中の相手。着信していなければ null で、着信カードを出さない。 */
    val incomingCall: IncomingCall? = null,
    val aircon: Aircon = Aircon(),
    /** 応答・拒否のコマンドを送っている間。二重に押されないようボタンを止める。 */
    val isSendingCommand: Boolean = false,
    /** どのコードからビルドしたか。テスターさんの報告と突き合わせるために画面の隅に出す。 */
    val buildInfo: BuildInfo = BuildInfo.UNKNOWN,
) : UiState
