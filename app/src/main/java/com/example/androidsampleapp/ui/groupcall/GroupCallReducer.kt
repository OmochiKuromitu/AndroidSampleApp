package com.example.androidsampleapp.ui.groupcall

import com.example.androidsampleapp.core.mvi.Reducer

/**
 * グループ通話の画面の (状態, Intent) -> 次の状態。
 *
 * 純粋関数に保つ。通話終了の Effect は GroupCallViewModel.handle が出す。
 */
class GroupCallReducer : Reducer<GroupCallState, GroupCallIntent> {
    override fun reduce(state: GroupCallState, intent: GroupCallIntent): GroupCallState = when (intent) {
        // スライダーは 0f〜1f を返すが、範囲は Reducer でも保証しておく。
        is GroupCallIntent.MicVolumeChanged -> state.copy(micVolume = intent.volume.coerceIn(0f, 1f))
        GroupCallIntent.SpeakerToggled -> state.copy(isSpeakerOn = !state.isSpeakerOn)
        GroupCallIntent.MuteToggled -> state.copy(isMuted = !state.isMuted)
        // 画面を閉じるのは状態の変化ではないので Effect で扱う。
        GroupCallIntent.EndCallClicked -> state
    }
}
