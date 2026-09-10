package com.example.androidsampleapp.ui.sleep

import com.example.androidsampleapp.core.mvi.Reducer

class SleepReducer : Reducer<SleepState, SleepIntent> {
    override fun reduce(state: SleepState, intent: SleepIntent): SleepState = when (intent) {
        is SleepIntent.Ticked -> state.copy(timeText = intent.timeText, dateText = intent.dateText)
        is SleepIntent.ConnectionStateChanged -> state.copy(connectionState = intent.state)
        is SleepIntent.NoticesChanged -> state.copy(notices = intent.notices)

        // 正規化は画面側でするが、範囲は Reducer でも保証しておく。
        is SleepIntent.UnlockDragged ->
            state.copy(unlockProgress = intent.progress.coerceIn(0f, 1f))

        SleepIntent.UnlockCancelled -> state.copy(unlockProgress = 0f)

        // 消去の結果は通知一覧の変化として戻ってくる。ここでは先読みしない。
        SleepIntent.ClearNoticesClicked -> state

        // 遷移は状態変化ではないので Effect で扱う。
        is SleepIntent.NoticeClicked -> state
    }
}
