package com.example.androidsampleapp.ui.main

import androidx.lifecycle.viewModelScope
import com.example.androidsampleapp.core.MissedCallManager
import com.example.androidsampleapp.core.mvi.MviViewModel
import com.example.androidsampleapp.domain.usecase.ObserveConnectionStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * メイン画面の枠が使う状態。ヘッダーの接続状態と、連絡先タブのバッジ件数を持つ。
 * 遷移には関与しない。
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    observeConnectionState: ObserveConnectionStateUseCase,
    missedCallManager: MissedCallManager,
) : MviViewModel<MainState, MainIntent, MainEffect>(
    initialState = MainState(),
    reducer = MainReducer(),
) {
    init {
        viewModelScope.launch {
            observeConnectionState().collect { dispatch(MainIntent.ConnectionStateChanged(it)) }
        }
        // 取得は MissedCallManager が行う。ここは件数を見るだけ。
        viewModelScope.launch {
            missedCallManager.missedCallCount.collect {
                dispatch(MainIntent.MissedCallCountChanged(it))
            }
        }
    }
}
