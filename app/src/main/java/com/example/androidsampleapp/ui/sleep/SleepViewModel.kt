package com.example.androidsampleapp.ui.sleep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidsampleapp.domain.model.Notice
import com.example.androidsampleapp.domain.usecase.ClearNoticesUseCase
import com.example.androidsampleapp.domain.usecase.GetNoticesUseCase
import com.example.androidsampleapp.domain.usecase.ObserveDeviceNoticesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * スリープ画面の ViewModel。
 *
 * - 1 秒ごとに時刻を整形して Ticked を投げる。
 * - 画面を開いたら API の通知を取り、機器から届く通知は購読する。
 * - 解除スワイプが必要な距離に届いた瞬間と、通知がタップされたときに Effect を出す。
 *   どこへ行くかは AppNavigation が決める。
 */
@HiltViewModel
class SleepViewModel @Inject constructor(
    private val getNotices: GetNoticesUseCase,
    private val clearNotices: ClearNoticesUseCase,
    observeDeviceNotices: ObserveDeviceNoticesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SleepState())
    val uiState: StateFlow<SleepState> = _uiState.asStateFlow()

    private val _effect = Channel<SleepEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private val reducer = SleepReducer()

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("M月d日 (E)", Locale.getDefault())

    init {
        // 機器からの通知は取りに行くものではなく降ってくるもの。購読して、届くたびに Intent にする。
        // StateFlow なので、スリープに入る前に届いていた分も購読した時点で流れてくる。
        viewModelScope.launch {
            observeDeviceNotices().collect { onIntent(SleepIntent.DeviceNoticesChanged(it)) }
        }
        viewModelScope.launch {
            while (isActive) {
                val now = Date()
                onIntent(SleepIntent.Ticked(timeFormat.format(now), dateFormat.format(now)))
                delay(TICK_INTERVAL_MS)
            }
        }
        onIntent(SleepIntent.Started)
    }

    /**
     * 状態を変えうる入力の入口。Route からの操作も、購読した値の変化も、通信の結果も、すべてここを通す。
     *
     * Reducer で次の状態を作って [_uiState] に入れ、そのあと [handle] で副作用を実行する。
     * 副作用は並行に走らせる。長い通信が、後から来た Intent の反映を止めないようにするため。
     */
    fun onIntent(intent: SleepIntent) {
        var previous: SleepState
        var current: SleepState
        // 読んでから書くまでの間に別の更新が入っていたら、読み直してやり直す。
        do {
            previous = _uiState.value
            current = reducer.reduce(previous, intent)
        } while (!_uiState.compareAndSet(previous, current))
        viewModelScope.launch { handle(intent, previous, current) }
    }

    private suspend fun handle(intent: SleepIntent, previous: SleepState, current: SleepState) {
        when (intent) {
            // スリープに入るたびに ViewModel ごと作り直されるので、取得もそのたびに走る。
            SleepIntent.Started -> load { getNotices() }

            // 消去は「消して取り直す」までが 1 つの操作。結果は取得と同じ経路で戻る。
            // 押しただけの ClearNoticesClicked では消さない（確認ダイアログを出すだけ）。
            SleepIntent.ClearNoticesConfirmed -> load { clearNotices() }

            // 到達した瞬間の 1 回だけ復帰させる。指がさらに動いても重ねて送らない。
            is SleepIntent.UnlockDragged ->
                if (!previous.isUnlockReached && current.isUnlockReached) {
                    _effect.send(SleepEffect.Unlocked)
                }

            is SleepIntent.NoticeClicked ->
                _effect.send(SleepEffect.NoticeSelected(intent.notice.destination))

            SleepIntent.ClearNoticesClicked,
            SleepIntent.ClearNoticesDismissed,
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
            .onSuccess { onIntent(SleepIntent.NoticesLoaded(it)) }
            .onFailure { onIntent(SleepIntent.NoticesLoadFailed) }
    }

    private companion object {
        const val TICK_INTERVAL_MS = 1_000L
    }
}
