package com.example.androidsampleapp.core.mvi

/** 画面の状態。UI はこれ「だけ」を描画する。必ず不変（data class）で表現する。 */
interface UiState

/** 状態を変えうる入力すべて。ユーザー操作に限らず、非同期処理の結果も Intent にする。 */
interface UiIntent

/** 状態として保持すべきでない一回きりの出来事（画面遷移、スナックバーなど）。 */
interface UiEffect

/**
 * (現在の状態, Intent) -> 次の状態 の純粋関数。
 * I/O、乱数、時刻取得、コルーチンの起動をここに書かないこと。
 */
fun interface Reducer<S : UiState, I : UiIntent> {
    fun reduce(state: S, intent: I): S
}
