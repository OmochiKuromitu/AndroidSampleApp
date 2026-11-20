package com.example.androidsampleapp.ui.groupcall

import com.example.androidsampleapp.core.mvi.UiIntent

/** グループ通話の画面の状態を変えうる入力の一覧。 */
sealed interface GroupCallIntent : UiIntent {
    /** マイク音量のスライダーを動かした。[volume] は 0f〜1f。 */
    data class MicVolumeChanged(val volume: Float) : GroupCallIntent

    /** スピーカーのボタンを押した。押すたびにオンとオフが入れ替わる。 */
    data object SpeakerToggled : GroupCallIntent

    /** ミュートのボタンを押した。押すたびにオンとオフが入れ替わる。 */
    data object MuteToggled : GroupCallIntent

    /** 通話終了のボタンを押した。画面を閉じるのは Effect を受けた AppNavigation。 */
    data object EndCallClicked : GroupCallIntent
}
