package com.example.androidsampleapp.ui.main

import androidx.lifecycle.viewModelScope
import com.example.androidsampleapp.core.mvi.MviViewModel
import com.example.androidsampleapp.domain.usecase.ObserveConnectionStateUseCase
import com.example.androidsampleapp.domain.usecase.ObserveIncomingCallUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    observeConnectionState: ObserveConnectionStateUseCase,
    observeIncomingCall: ObserveIncomingCallUseCase,
) : MviViewModel<MainState, MainIntent, MainEffect>(
    initialState = MainState(),
    reducer = MainReducer(),
) {

    init {
        // 共有状態の変化も Intent に変換して Reducer に通す。状態変化の入口を 1 か所に保つため。
        viewModelScope.launch {
            observeConnectionState().collect { dispatch(MainIntent.ConnectionStateChanged(it)) }
        }
        viewModelScope.launch {
            observeIncomingCall().filterNotNull().collect { dispatch(MainIntent.IncomingCallReceived) }
        }
    }

    override suspend fun handle(intent: MainIntent, previous: MainState, current: MainState) {
        when (intent) {
            is MainIntent.TabClicked -> when {
                intent.tab == MainTab.SLEEP -> sendEffect(MainEffect.NavigateToSleep)
                previous.selectedTab == intent.tab -> sendEffect(MainEffect.PopToTabRoot(intent.tab))
                else -> sendEffect(MainEffect.NavigateToTab(intent.tab))
            }

            MainIntent.IncomingCallReceived ->
                if (previous.selectedTab != MainTab.TOP) sendEffect(MainEffect.NavigateToTab(MainTab.TOP))

            // NavController が先に動いた結果の同期なので、ここから再遷移しない（ループ防止）。
            is MainIntent.BackStackChanged,
            is MainIntent.ConnectionStateChanged,
            -> Unit
        }
    }
}
