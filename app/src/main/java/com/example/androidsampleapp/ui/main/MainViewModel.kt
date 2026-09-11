package com.example.androidsampleapp.ui.main

import androidx.lifecycle.viewModelScope
import com.example.androidsampleapp.core.mvi.MviViewModel
import com.example.androidsampleapp.domain.usecase.ObserveConnectionStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * メイン画面の枠が使う状態。ヘッダーに出す接続状態だけを持つ。
 * 遷移には関与しない。
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    observeConnectionState: ObserveConnectionStateUseCase,
) : MviViewModel<MainState, MainIntent, MainEffect>(
    initialState = MainState(),
    reducer = MainReducer(),
) {
    init {
        viewModelScope.launch {
            observeConnectionState().collect { dispatch(MainIntent.ConnectionStateChanged(it)) }
        }
    }
}
