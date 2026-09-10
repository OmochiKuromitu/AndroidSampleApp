package com.example.androidsampleapp.ui.sleep

import com.example.androidsampleapp.core.mvi.UiIntent
import com.example.androidsampleapp.domain.model.ConnectionState
import com.example.androidsampleapp.domain.model.Notice

sealed interface SleepIntent : UiIntent {
    data class Ticked(val timeText: String, val dateText: String) : SleepIntent
    data class ConnectionStateChanged(val state: ConnectionState) : SleepIntent

    /** 通知一覧の変化。init での受け取りも、その後の追加もこれで入る。 */
    data class NoticesChanged(val notices: List<Notice>) : SleepIntent

    data class NoticeClicked(val notice: Notice) : SleepIntent
    data object ClearNoticesClicked : SleepIntent

    /** 下端から上へスワイプ中。[progress] は 0f..1f に正規化済み。 */
    data class UnlockDragged(val progress: Float) : SleepIntent

    /** 解除に届かないまま指が離れた、あるいはジェスチャが中断された。 */
    data object UnlockCancelled : SleepIntent
}
