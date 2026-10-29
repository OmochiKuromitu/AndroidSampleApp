package com.example.androidsampleapp.ui.navigation

import androidx.lifecycle.ViewModel
import com.example.androidsampleapp.core.AppStateHolder
import com.example.androidsampleapp.domain.model.IncomingCall
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/**
 * 着信を Composable から見るための窓口。[IncomingCallRouter] が使う。
 *
 * スリープとは別の責務なので、まとめずに分けてある。
 */
@HiltViewModel
class IncomingCallViewModel @Inject constructor(
    private val appStateHolder: AppStateHolder,
) : ViewModel() {

    val incomingCall: StateFlow<IncomingCall?> = appStateHolder.incomingCall

    private var activeRoomId: String? = null
    private var returnToSleep = false

    // 再コンポーズや表示名の更新では遷移しない。画面再生成でも着信前の状態を保持する。
    fun onCallChanged(call: IncomingCall?, isSleeping: Boolean): CallNavigation? {
        if (call != null) {
            if (call.roomId == activeRoomId) return null
            returnToSleep = if (activeRoomId == null) isSleeping else returnToSleep
            activeRoomId = call.roomId
            return CallNavigation.SHOW_CALL
        }
        val roomId = activeRoomId ?: return null
        val shouldSleep = returnToSleep && appStateHolder.answeredRoomId != roomId
        activeRoomId = null
        returnToSleep = false
        return if (shouldSleep) CallNavigation.RETURN_TO_SLEEP else null
    }
}

enum class CallNavigation { SHOW_CALL, RETURN_TO_SLEEP }
