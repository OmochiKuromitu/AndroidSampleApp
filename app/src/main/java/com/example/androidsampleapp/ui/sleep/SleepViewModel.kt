package com.example.androidsampleapp.ui.sleep

import androidx.lifecycle.viewModelScope
import com.example.androidsampleapp.core.mvi.MviViewModel
import com.example.androidsampleapp.domain.model.Notice
import com.example.androidsampleapp.domain.usecase.ClearNoticesUseCase
import com.example.androidsampleapp.domain.usecase.GetNoticesUseCase
import com.example.androidsampleapp.domain.usecase.ObserveDeviceNoticesUseCase
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
    private val getNotices: GetNoticesUseCase,
    private val clearNotices: ClearNoticesUseCase,
    observeDeviceNotices: ObserveDeviceNoticesUseCase,
) : MviViewModel<SleepState, SleepIntent, SleepEffect>(
    initialState = SleepState(),
    reducer = SleepReducer(),
) {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("M月d日 (E)", Locale.getDefault())

    init {
        // 機器からの通知は取りに行くものではなく降ってくるもの。購読して、届くたびに Intent にする。
        // StateFlow なので、スリープに入る前に届いていた分も購読した時点で流れてくる。
        viewModelScope.launch {
            observeDeviceNotices().collect { dispatch(SleepIntent.DeviceNoticesChanged(it)) }
        }
        viewModelScope.launch {
            while (isActive) {
                val now = Date()
                dispatch(SleepIntent.Ticked(timeFormat.format(now), dateFormat.format(now)))
                delay(TICK_INTERVAL_MS)
            }
        }
        dispatch(SleepIntent.Started)
    }

    override suspend fun handle(intent: SleepIntent, previous: SleepState, current: SleepState) {
        when (intent) {
            // スリープに入るたびに ViewModel ごと作り直されるので、取得もそのたびに走る。
            SleepIntent.Started -> load { getNotices() }

            // 消去は「消して取り直す」までが 1 つの操作。結果は取得と同じ経路で戻る。
            SleepIntent.ClearNoticesClicked -> load { clearNotices() }

            // 到達した瞬間の 1 回だけ復帰させる。指がさらに動いても重ねて送らない。
            is SleepIntent.UnlockDragged ->
                if (!previous.isUnlockReached && current.isUnlockReached) {
                    sendEffect(SleepEffect.Unlocked)
                }

            is SleepIntent.NoticeClicked ->
                sendEffect(SleepEffect.NoticeSelected(intent.notice.destination))

            SleepIntent.UnlockCancelled,
            is SleepIntent.NoticesLoaded,
            SleepIntent.NoticesLoadFailed,
            is SleepIntent.DeviceNoticesChanged,
            is SleepIntent.Ticked,
            -> Unit
        }
    }

    private suspend fun load(fetch: suspend () -> List<Notice>) {
        runCatching { fetch() }
            .onSuccess { dispatch(SleepIntent.NoticesLoaded(it)) }
            .onFailure { dispatch(SleepIntent.NoticesLoadFailed) }
    }

    private companion object {
        const val TICK_INTERVAL_MS = 1_000L
    }
}
