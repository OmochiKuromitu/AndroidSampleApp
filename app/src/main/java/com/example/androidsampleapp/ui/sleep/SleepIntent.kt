package com.example.androidsampleapp.ui.sleep

import com.example.androidsampleapp.core.NoticeSnapshot
import com.example.androidsampleapp.core.mvi.UiIntent
import com.example.androidsampleapp.domain.model.Notice

sealed interface SleepIntent : UiIntent {
    data class Ticked(val timeText: String, val dateText: String) : SleepIntent

    /** NoticeManager の一覧の変化。取得・消去の結果も、機器から届いた通知もここに戻る。 */
    data class NoticesChanged(val snapshot: NoticeSnapshot) : SleepIntent

    data object ClearNoticesClicked : SleepIntent
    data class NoticeClicked(val notice: Notice) : SleepIntent

    /** 下端から上へスワイプ中。[progress] は 0f..1f に正規化済み。 */
    data class UnlockDragged(val progress: Float) : SleepIntent

    /** 解除に届かないまま指が離れた、あるいはジェスチャが中断された。 */
    data object UnlockCancelled : SleepIntent
}
