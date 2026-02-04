package com.example.androidsampleapp.ui.main

import com.example.androidsampleapp.core.mvi.Reducer

/**
 * メイン画面の枠の (状態, Intent) -> 次の状態。受け取った値を写すだけ。
 */
class MainReducer : Reducer<MainState, MainIntent> {
    override fun reduce(state: MainState, intent: MainIntent): MainState = when (intent) {
        is MainIntent.ConnectionStateChanged -> state.copy(connectionState = intent.state)
        is MainIntent.MissedCallCountChanged -> state.copy(missedCallCount = intent.count)
    }
}
