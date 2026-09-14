package com.example.androidsampleapp.ui.sleep

import androidx.lifecycle.viewModelScope
import com.example.androidsampleapp.core.NoticeManager
import com.example.androidsampleapp.core.mvi.MviViewModel
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
    private val noticeManager: NoticeManager,
) : MviViewModel<SleepState, SleepIntent, SleepEffect>(
    initialState = SleepState(),
    reducer = SleepReducer(),
) {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("M月d日 (E)", Locale.getDefault())

    init {
        // 取得は NoticeManager が行い、きっかけは AppNavigation が決める。ここは一覧を見るだけ。
        // StateFlow なので、スリープに入る前に取れていた分も購読した時点で流れてくる。
        viewModelScope.launch {
            noticeManager.snapshot.collect { dispatch(SleepIntent.NoticesChanged(it)) }
        }
        viewModelScope.launch {
            while (isActive) {
                val now = Date()
                dispatch(SleepIntent.Ticked(timeFormat.format(now), dateFormat.format(now)))
                delay(TICK_INTERVAL_MS)
            }
        }
    }

    override suspend fun handle(intent: SleepIntent, previous: SleepState, current: SleepState) {
        when (intent) {
            // 結果は NoticeManager の一覧の変化として NoticesChanged で戻る。
            SleepIntent.ClearNoticesClicked -> noticeManager.clear()

            // 到達した瞬間の 1 回だけ復帰させる。指がさらに動いても重ねて送らない。
            is SleepIntent.UnlockDragged ->
                if (!previous.isUnlockReached && current.isUnlockReached) {
                    sendEffect(SleepEffect.Unlocked)
                }

            is SleepIntent.NoticeClicked ->
                sendEffect(SleepEffect.NoticeSelected(intent.notice.destination))

            SleepIntent.UnlockCancelled,
            is SleepIntent.NoticesChanged,
            is SleepIntent.Ticked,
            -> Unit
        }
    }

    private companion object {
        const val TICK_INTERVAL_MS = 1_000L
    }
}
