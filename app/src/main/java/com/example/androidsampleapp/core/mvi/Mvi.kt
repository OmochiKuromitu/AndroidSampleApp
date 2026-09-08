package com.example.androidsampleapp.core.mvi

/**
 * 画面の状態。UI はこれ「だけ」を描画する。必ず不変（data class）で表現する。
 */
interface UiState

/**
 * ユーザー操作や非同期処理の結果など、状態を変えうる入力すべて。
 */
interface UiIntent

/**
 * 状態として保持すべきでない一回きりの出来事（画面遷移、スナックバー表示など）。
 */
interface UiEffect

/**
 * (現在の状態, Intent) -> 次の状態 の純粋関数。
 * I/O、乱数、時刻取得、コルーチンの起動をここに書かないこと。テスト容易性はこの制約で担保される。
 */
fun interface Reducer<S : UiState, I : UiIntent> {
    fun reduce(state: S, intent: I): S
}
