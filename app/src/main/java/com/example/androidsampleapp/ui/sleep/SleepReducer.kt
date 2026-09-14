package com.example.androidsampleapp.ui.sleep

import com.example.androidsampleapp.core.mvi.Reducer

/**
 * スリープ画面の (状態, Intent) -> 次の状態。
 *
 * 純粋関数に保つ。時刻の取得、NoticeManager への消去の依頼、解除の判定に応じた Effect は
 * SleepViewModel が行う。
 */
class SleepReducer : Reducer<SleepState, SleepIntent> {
    override fun reduce(state: SleepState, intent: SleepIntent): SleepState = when (intent) {
        is SleepIntent.NoticesChanged -> state.copy(
            notices = intent.snapshot.notices,
            isLoadingNotices = intent.snapshot.isLoading,
            noticeLoadFailed = intent.snapshot.loadFailed,
        )

        is SleepIntent.Ticked -> state.copy(timeText = intent.timeText, dateText = intent.dateText)

        // 正規化は画面側でするが、範囲は Reducer でも保証しておく。
        is SleepIntent.UnlockDragged ->
            state.copy(unlockProgress = intent.progress.coerceIn(0f, 1f))

        SleepIntent.UnlockCancelled -> state.copy(unlockProgress = 0f)

        // 読み込み中かどうかは NoticeManager が決めて NoticesChanged で戻る。先読みしない。
        SleepIntent.ClearNoticesClicked -> state

        // 遷移は状態変化ではないので Effect で扱う。
        is SleepIntent.NoticeClicked -> state
    }
}
