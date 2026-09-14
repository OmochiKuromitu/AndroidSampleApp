package com.example.androidsampleapp.ui.sleep

import com.example.androidsampleapp.core.mvi.Reducer

class SleepReducer : Reducer<SleepState, SleepIntent> {
    override fun reduce(state: SleepState, intent: SleepIntent): SleepState = when (intent) {
        // 消去も取り直しを伴うので、取得と同じく読み込み中にする。
        SleepIntent.Started,
        SleepIntent.ClearNoticesClicked,
        -> state.copy(isLoadingNotices = true, noticeLoadFailed = false)

        is SleepIntent.NoticesLoaded -> state.copy(
            apiNotices = intent.notices,
            isLoadingNotices = false,
            noticeLoadFailed = false,
        )

        SleepIntent.NoticesLoadFailed ->
            state.copy(isLoadingNotices = false, noticeLoadFailed = true)

        is SleepIntent.DeviceNoticesChanged -> state.copy(deviceNotices = intent.notices)

        is SleepIntent.Ticked -> state.copy(timeText = intent.timeText, dateText = intent.dateText)

        // 正規化は画面側でするが、範囲は Reducer でも保証しておく。
        is SleepIntent.UnlockDragged ->
            state.copy(unlockProgress = intent.progress.coerceIn(0f, 1f))

        SleepIntent.UnlockCancelled -> state.copy(unlockProgress = 0f)

        // 遷移は状態変化ではないので Effect で扱う。
        is SleepIntent.NoticeClicked -> state
    }
}
