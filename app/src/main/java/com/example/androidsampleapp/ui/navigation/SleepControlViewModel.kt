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
 * 画面ではないので MVI（State / Intent / Effect）は敷かない。状態は [IdleTimer] に
 * あり、ここは素通しに徹する。
 */
@HiltViewModel
class SleepControlViewModel @Inject constructor(
    private val idleTimer: IdleTimer,
) : ViewModel() {

    val isSleeping: StateFlow<Boolean> = idleTimer.isSleeping

    fun onInteraction() = idleTimer.onInteraction()

    fun wake() = idleTimer.wake()

    fun onSleepRequested() = idleTimer.onSleepRequested()
}
