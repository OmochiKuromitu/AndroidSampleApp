package com.example.androidsampleapp.ui.sleep

import androidx.lifecycle.viewModelScope
import com.example.androidsampleapp.core.mvi.MviViewModel
import com.example.androidsampleapp.domain.usecase.ClearNoticesUseCase
import com.example.androidsampleapp.domain.usecase.ObserveConnectionStateUseCase
import com.example.androidsampleapp.domain.usecase.ObserveNoticesUseCase
import com.example.androidsampleapp.domain.usecase.RefreshNoticesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@HiltViewModel
class SleepViewModel @Inject constructor(
    observeConnectionState: ObserveConnectionStateUseCase,
    observeNotices: ObserveNoticesUseCase,
    private val refreshNotices: RefreshNoticesUseCase,
    private val clearNotices: ClearNoticesUseCase,
) : MviViewModel<SleepState, SleepIntent, SleepEffect>(
    initialState = SleepState(),
    reducer = SleepReducer(),
) {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("M月d日 (E)", Locale.getDefault())

    init {
        viewModelScope.launch {
            while (isActive) {
                val now = Date()
                dispatch(SleepIntent.Ticked(timeFormat.format(now), dateFormat.format(now)))
                delay(TICK_INTERVAL_MS)
            }
        }
        viewModelScope.launch {
            observeConnectionState().collect { dispatch(SleepIntent.ConnectionStateChanged(it)) }
        }
        // 保持している一覧を購読する。取得も消去も結果はここに流れてくる。
        viewModelScope.launch {
            observeNotices().collect { dispatch(SleepIntent.NoticesChanged(it)) }
        }
        dispatch(SleepIntent.Started)
    }

    override suspend fun handle(intent: SleepIntent, previous: SleepState, current: SleepState) {
        when (intent) {
            // スリープに入るたびに ViewModel ごと作り直されるので、取得もそのたびに走る。
            SleepIntent.Started -> loadNotices()

            SleepIntent.ClearNoticesClicked -> clearNotices()

            // 到達した瞬間の 1 回だけ復帰させる。指がさらに動いても重ねて送らない。
            is SleepIntent.UnlockDragged ->
                if (!previous.isUnlockReached && current.isUnlockReached) {
                    sendEffect(SleepEffect.Wake)
                }

            is SleepIntent.NoticeClicked ->
                sendEffect(SleepEffect.OpenDestination(intent.notice.destination))

            SleepIntent.UnlockCancelled,
            SleepIntent.NoticesLoaded,
            SleepIntent.NoticesLoadFailed,
            is SleepIntent.Ticked,
            is SleepIntent.ConnectionStateChanged,
            is SleepIntent.NoticesChanged,
            -> Unit
        }
    }

    private suspend fun loadNotices() {
        runCatching { refreshNotices() }
            .onSuccess { dispatch(SleepIntent.NoticesLoaded) }
            .onFailure { dispatch(SleepIntent.NoticesLoadFailed) }
    }

    private companion object {
        const val TICK_INTERVAL_MS = 1_000L
    }
}
