package com.example.androidsampleapp.ui.sleep

import com.example.androidsampleapp.core.mvi.UiIntent
import com.example.androidsampleapp.domain.model.ConnectionState
import com.example.androidsampleapp.domain.model.Notice

sealed interface SleepIntent : UiIntent {
    /** 画面の生成時に 1 度だけ流す。ここで通知を取りに行く。 */
    data object Started : SleepIntent

    data class Ticked(val timeText: String, val dateText: String) : SleepIntent
    data class ConnectionStateChanged(val state: ConnectionState) : SleepIntent

    /** 通知一覧の変化。取得結果も消去結果もここを通る。 */
    data class NoticesChanged(val notices: List<Notice>) : SleepIntent

    data object NoticesLoaded : SleepIntent
    data object NoticesLoadFailed : SleepIntent

    data class NoticeClicked(val notice: Notice) : SleepIntent
    data object ClearNoticesClicked : SleepIntent

    /** 下端から上へスワイプ中。[progress] は 0f..1f に正規化済み。 */
    data class UnlockDragged(val progress: Float) : SleepIntent

    /** 解除に届かないまま指が離れた、あるいはジェスチャが中断された。 */
    data object UnlockCancelled : SleepIntent
}
