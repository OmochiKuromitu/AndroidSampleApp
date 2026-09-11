package com.example.androidsampleapp.ui.sleep

import com.example.androidsampleapp.core.mvi.UiIntent
import com.example.androidsampleapp.domain.model.Notice

sealed interface SleepIntent : UiIntent {
    /** 画面の生成時に 1 度だけ流す。ここで通知を取りに行く。 */
    data object Started : SleepIntent

    data class Ticked(val timeText: String, val dateText: String) : SleepIntent

    /** 取得の結果。消去の結果もここに戻る（消去は取り直しを伴うため）。 */
    data class NoticesLoaded(val notices: List<Notice>) : SleepIntent
    data object NoticesLoadFailed : SleepIntent

    data object ClearNoticesClicked : SleepIntent
    data class NoticeClicked(val notice: Notice) : SleepIntent

    /** 下端から上へスワイプ中。[progress] は 0f..1f に正規化済み。 */
    data class UnlockDragged(val progress: Float) : SleepIntent

    /** 解除に届かないまま指が離れた、あるいはジェスチャが中断された。 */
    data object UnlockCancelled : SleepIntent
}
