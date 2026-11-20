package com.example.androidsampleapp.ui.groupcall

import com.example.androidsampleapp.core.mvi.UiState
import com.example.androidsampleapp.domain.model.GroupCallMember

/**
 * グループ通話の画面の状態。参加しているメンバーと、マイク音量・スピーカー・ミュートの操作の状態を持つ。
 *
 * 画面を仮置きした段階。音量・スピーカー・ミュートは画面の中で値が変わるだけで、通話にはまだ反映しない。
 */
data class GroupCallState(
    /** 参加しているメンバー。自分は含めない。 */
    val members: List<GroupCallMember> = emptyList(),
    /** マイク音量。0f〜1f。 */
    val micVolume: Float = DEFAULT_MIC_VOLUME,
    /** スピーカーで鳴らしているか。 */
    val isSpeakerOn: Boolean = false,
    /** 自分のマイクを切っているか。 */
    val isMuted: Boolean = false,
) : UiState {
    /** 上の緑のラベルに出す人数。 */
    val memberCount: Int
        get() = members.size

    companion object {
        const val DEFAULT_MIC_VOLUME = 0.5f
    }
}
