package com.example.androidsampleapp.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidsampleapp.core.MissedCallManager
import com.example.androidsampleapp.domain.usecase.ObserveConnectionStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * メイン画面の枠が使う状態。ヘッダーの接続状態と、連絡先タブのバッジ件数を持つ。
 * 遷移には関与しない。
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    observeConnectionState: ObserveConnectionStateUseCase,
    missedCallManager: MissedCallManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainState())
    val uiState: StateFlow<MainState> = _uiState.asStateFlow()

    private val _effect = Channel<MainEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private val reducer = MainReducer()

    init {
        viewModelScope.launch {
            observeConnectionState().collect { onIntent(MainIntent.ConnectionStateChanged(it)) }
        }
        // 取得は MissedCallManager が行う。ここは件数を見るだけ。
        viewModelScope.launch {
            missedCallManager.missedCallCount.collect {
                onIntent(MainIntent.MissedCallCountChanged(it))
            }
        }
    }

    /**
     * 状態を変えうる入力の入口。Route からの操作も、購読した値の変化も、通信の結果も、すべてここを通す。
     *
     * Reducer で次の状態を作って [_uiState] に入れる。この画面には副作用が無い。
     */
    fun onIntent(intent: MainIntent) {
        // 副作用で前後の状態を見比べないので、update で足りる（読み直しとやり直しは update が行う）。
        _uiState.update { reducer.reduce(it, intent) }
    }
}
