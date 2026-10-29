package com.example.androidsampleapp.ui.navigation

import com.example.androidsampleapp.core.AppStateHolder
import com.example.androidsampleapp.domain.model.IncomingCall
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IncomingCallViewModelTest {
    private val holder = AppStateHolder()
    private val viewModel = IncomingCallViewModel(holder)
    private val call = IncomingCall("room-1", "受付")

    @Test
    fun missedCallReturnsToSleepOnlyOnce() {
        assertNull(viewModel.onCallChanged(null, true))
        assertEquals(CallNavigation.SHOW_CALL, viewModel.onCallChanged(call, true))
        assertEquals(CallNavigation.RETURN_TO_SLEEP, viewModel.onCallChanged(null, false))
        assertNull(viewModel.onCallChanged(null, true))
    }

    @Test
    fun answeredCallDoesNotReturnToSleep() {
        holder.updateIncomingCall(call)
        viewModel.onCallChanged(call, true)
        holder.markCallAnswered(call.roomId)
        holder.updateIncomingCall(null)
        assertNull(viewModel.onCallChanged(null, false))
    }

    @Test
    fun callReceivedWhileAwakeDoesNotReturnToSleep() {
        viewModel.onCallChanged(call, false)
        assertNull(viewModel.onCallChanged(null, false))
    }

    @Test
    fun callerNameUpdatePreservesOriginalSleepState() {
        viewModel.onCallChanged(call, true)
        assertNull(viewModel.onCallChanged(call.copy(displayName = "玄関"), false))
        assertEquals(CallNavigation.RETURN_TO_SLEEP, viewModel.onCallChanged(null, false))
    }

    @Test
    fun nextCallInSameRoomDoesNotInheritPreviousAnswer() {
        holder.updateIncomingCall(call)
        viewModel.onCallChanged(call, true)
        holder.markCallAnswered(call.roomId)
        holder.updateIncomingCall(null)
        viewModel.onCallChanged(null, false)
        holder.updateIncomingCall(call)
        viewModel.onCallChanged(call, true)
        holder.updateIncomingCall(null)
        assertEquals(CallNavigation.RETURN_TO_SLEEP, viewModel.onCallChanged(null, false))
    }
}
