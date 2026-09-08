package com.example.androidsampleapp.ui.sleep

import com.example.androidsampleapp.core.mvi.Reducer

class SleepReducer : Reducer<SleepState, SleepIntent> {
    override fun reduce(state: SleepState, intent: SleepIntent): SleepState = when (intent) {
        is SleepIntent.Ticked -> state.copy(timeText = intent.timeText, dateText = intent.dateText)
        is SleepIntent.ConnectionStateChanged -> state.copy(connectionState = intent.state)

        // 復帰は状態変化ではないので Effect で扱う。
        SleepIntent.ScreenTapped -> state
    }
}
