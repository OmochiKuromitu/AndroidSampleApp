package com.example.androidsampleapp.ui.groupcall

import com.example.androidsampleapp.domain.model.GroupCallMember
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupCallReducerTest {

    private val reducer = GroupCallReducer()

    private val members = listOf(
        GroupCallMember(id = "1", name = "1号車運転台", device = "ハンドセット"),
        GroupCallMember(id = "2", name = "添乗員端末2", device = "車両連絡"),
    )

    @Test
    fun `人数はメンバーの数`() {
        assertEquals(2, GroupCallState(members = members).memberCount)
    }

    @Test
    fun `マイク音量が入る`() {
        val next = reducer.reduce(GroupCallState(), GroupCallIntent.MicVolumeChanged(0.8f))

        assertEquals(0.8f, next.micVolume, TOLERANCE)
    }

    @Test
    fun `マイク音量は 0f から 1f に収める`() {
        assertEquals(1f, reducer.reduce(GroupCallState(), GroupCallIntent.MicVolumeChanged(1.5f)).micVolume, TOLERANCE)
        assertEquals(0f, reducer.reduce(GroupCallState(), GroupCallIntent.MicVolumeChanged(-0.5f)).micVolume, TOLERANCE)
    }

    @Test
    fun `スピーカーとミュートは押すたびに入れ替わる`() {
        val speakerOn = reducer.reduce(GroupCallState(), GroupCallIntent.SpeakerToggled)
        assertTrue(speakerOn.isSpeakerOn)
        assertFalse(reducer.reduce(speakerOn, GroupCallIntent.SpeakerToggled).isSpeakerOn)

        val muted = reducer.reduce(GroupCallState(), GroupCallIntent.MuteToggled)
        assertTrue(muted.isMuted)
        assertFalse(reducer.reduce(muted, GroupCallIntent.MuteToggled).isMuted)
    }

    @Test
    fun `通話終了を押しても状態は変えない`() {
        // 画面を閉じるのは Effect を受けた AppNavigation。
        val state = GroupCallState(members = members, isMuted = true)

        val next = reducer.reduce(state, GroupCallIntent.EndCallClicked)

        assertEquals(state, next)
    }

    private companion object {
        const val TOLERANCE = 0.0001f
    }
}
