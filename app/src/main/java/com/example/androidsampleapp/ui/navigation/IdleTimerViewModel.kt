package com.example.androidsampleapp.ui.navigation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/**
 * スリープの状態と操作を Composable から触るための窓口。中身は [IdleTimer] が持つ。
 *
 * Composable には @Inject できないため、誰かが依存を渡す必要がある。ここでは
 * 「スリープ」という責務 1 つに絞った ViewModel を置き、[AppNavigation] が取る。
 *
 * 「AppNavigation が必要とするもの」でまとめないのは、それが責務ではないから。
 * 基準が無い入れ物は、画面が増えるたびに無関係なものが同居して太る。
 *
 * タイマー本体を ViewModel ではなく [IdleTimer]（@Singleton）に置いているのは、
 * 画面消灯とバックグラウンド移行を Application が拾う必要があるため。
 * ViewModel には Application から触れない。
 *
 * 画面ではないので MVI（State / Intent / Effect）は敷かない。状態は [IdleTimer] に
 * あり、ここは素通しに徹する。
 */
@HiltViewModel
class IdleTimerViewModel @Inject constructor(
    private val idleTimer: IdleTimer,
) : ViewModel() {

    val isSleeping: StateFlow<Boolean> = idleTimer.isSleeping

    /** ユーザー操作を検知したら呼ぶ。無操作タイマーを測り直す。 */
    fun onInteraction() = idleTimer.onInteraction()

    /** スリープを解除する。解除操作や着信など、明示的に起こしたいときに呼ぶ。 */
    fun wake() = idleTimer.wake()

    /** スリープタブが選ばれたときに呼ぶ。 */
    fun onSleepRequested() = idleTimer.onSleepRequested()
}
