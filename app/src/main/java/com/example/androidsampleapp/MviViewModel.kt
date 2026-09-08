package com.example.androidsampleapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

/**
 * MVI の土台。
 *
 * 流れは常に一方向:
 *   UI -> dispatch(Intent) -> Reducer で状態更新 -> state を UI が描画
 *                          -> handle(...) で副作用（I/O・Effect 送出・追加 Intent）
 *
 * Reducer は純粋関数なので、副作用は [handle] 側に隔離する。
 */
abstract class MviViewModel<S : UiState, I : UiIntent, E : UiEffect>(
    initialState: S,
    private val reducer: Reducer<S, I>,
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    private val _effect = Channel<E>(Channel.BUFFERED)
    val effect: Flow<E> = _effect.receiveAsFlow()

    /** Intent は 1 本のチャネルに集約し、到着順に 1 つずつ reduce する。 */
    private val intents = Channel<I>(Channel.UNLIMITED)

    init {
        viewModelScope.launch {
            // サブクラスの init から dispatch されても、構築が終わってから reduce する。
            yield()
            for (intent in intents) {
                val previous = _state.value
                val current = reducer.reduce(previous, intent)
                _state.value = current
                // 副作用は並行実行。長い I/O が後続 Intent の reduce を止めないようにする。
                launch { handle(intent, previous, current) }
            }
        }
    }

    fun dispatch(intent: I) {
        intents.trySend(intent)
    }

    /**
     * reduce 後に呼ばれる副作用のフック。
     * 通信・DB アクセス・Effect 送出はここで行い、結果は再び [dispatch] して Reducer に戻す。
     */
    protected open suspend fun handle(intent: I, previous: S, current: S) = Unit

    protected suspend fun sendEffect(effect: E) {
        _effect.send(effect)
    }
}
