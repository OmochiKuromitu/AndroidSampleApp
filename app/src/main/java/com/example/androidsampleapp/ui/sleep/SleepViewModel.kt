package com.example.androidsampleapp.ui.sleep

import androidx.lifecycle.viewModelScope
import com.example.androidsampleapp.core.mvi.MviViewModel
import com.example.androidsampleapp.domain.usecase.ObserveConnectionStateUseCase
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
    }

    override suspend fun handle(intent: SleepIntent, previous: SleepState, current: SleepState) {
        when (intent) {
            SleepIntent.ScreenTapped -> sendEffect(SleepEffect.Wake)
            is SleepIntent.Ticked,
            is SleepIntent.ConnectionStateChanged,
            -> Unit
        }
    }

    private companion object {
        const val TICK_INTERVAL_MS = 1_000L
    }
}
