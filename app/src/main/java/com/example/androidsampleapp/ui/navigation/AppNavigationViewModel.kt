package com.example.androidsampleapp.ui.navigation

import androidx.lifecycle.ViewModel
import com.example.androidsampleapp.core.AppStateHolder
import com.example.androidsampleapp.domain.model.IncomingCall
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/**
 * [AppNavigation] が必要とするものだけを渡す窓口。
 *
 * Composable には @Inject できないため、誰かが依存を渡す必要がある。
 * Activity に持たせて引数で降ろすこともできるが、そうすると遷移に必要なものが
 * 増えるたびに MainActivity が太る。ViewModel にしておけば AppNavigation 側で閉じる。
 *
 * 画面ではないので MVI（State / Intent / Effect）は敷かない。
 * 状態は [IdleTimer] と [AppStateHolder] が持っており、ここは素通しの窓口に徹する。
 */
@HiltViewModel
class AppNavigationViewModel @Inject constructor(
    private val idleTimer: IdleTimer,
    appStateHolder: AppStateHolder,
) : ViewModel() {

    val isSleeping: StateFlow<Boolean> = idleTimer.isSleeping
    val incomingCall: StateFlow<IncomingCall?> = appStateHolder.incomingCall

    fun onInteraction() = idleTimer.onInteraction()

    fun wake() = idleTimer.wake()

    fun onSleepRequested() = idleTimer.onSleepRequested()
}
