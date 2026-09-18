package com.example.androidsampleapp.ui.sleep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidsampleapp.domain.usecase.ClearNoticesUseCase
import com.example.androidsampleapp.domain.usecase.ObserveDeviceNoticesUseCase
import com.example.androidsampleapp.domain.usecase.ObserveNoticesUseCase
import com.example.androidsampleapp.domain.usecase.RefreshNoticesUseCase
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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * スリープ画面の ViewModel。
 *
 * - 1 秒ごとに時刻を整形して Ticked を投げる。
 * - API の通知は NoticeRepository を、機器から届く通知は AppStateHolder を購読し、値が変わるたびに Intent にする。
 *   画面を開いたら取り直しを頼み、失敗したときだけ Intent で戻す（成功した結果は購読側に流れる）。
 * - 解除スワイプが必要な距離に届いた瞬間と、通知がタップされたときに Effect を出す。
 *   どこへ行くかは AppNavigation が決める。
 */
@HiltViewModel
class SleepViewModel @Inject constructor(
    observeNotices: ObserveNoticesUseCase,
    observeDeviceNotices: ObserveDeviceNoticesUseCase,
    private val refreshNotices: RefreshNoticesUseCase,
    private val clearNotices: ClearNoticesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SleepState())
    val uiState: StateFlow<SleepState> = _uiState.asStateFlow()

    private val _effect = Channel<SleepEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private val reducer = SleepReducer()

    // ロケールは端末任せにしない。パターンに「月」「日」を直接書いているので、
    // 端末が日本語以外だと曜日だけ "Wed" になって混ざる。壁付けパネルは日本向けに固定でよい。
    private val timeFormat = SimpleDateFormat("H:mm:ss", Locale.JAPAN)
    private val dateFormat = SimpleDateFormat("M月d日 (E)", Locale.JAPAN)

    init {
        // API の通知。まだ一度も取れていない間は null が流れる。読み込み中の表示は State が決めるので、ここでは捨てる。
        viewModelScope.launch {
            observeNotices().filterNotNull().collect { onIntent(SleepIntent.ApiNoticesChanged(it)) }
        }
        // 機器からの通知は取りに行くものではなく降ってくるもの。購読して、届くたびに Intent にする。
        // StateFlow なので、スリープに入る前に届いていた分も購読した時点で流れてくる。
        viewModelScope.launch {
            observeDeviceNotices().collect { onIntent(SleepIntent.DeviceNoticesChanged(it)) }
        }
        viewModelScope.launch {
            while (isActive) {
                val now = Date()
                onIntent(SleepIntent.Ticked(timeFormat.format(now), dateFormat.format(now)))
                // 秒を出すので、次の秒の頭に合わせて起こす。固定で 1 秒待つと整形と再開の分だけ
                // 少しずつ後ろへずれて、表示上の秒が飛んだり同じ値が 2 回続いたりする。
                delay(TICK_INTERVAL_MS - now.time % TICK_INTERVAL_MS)
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
            // スリープに入るたびに ViewModel ごと作り直されるので、取り直しもそのたびに走る。
            SleepIntent.Started -> runNoticeRequest { refreshNotices() }

            // 消去は「消して取り直す」までが 1 つの操作。結果は取得と同じく購読側に流れる。
            // 押しただけの ClearNoticesClicked では消さない（確認ダイアログを出すだけ）。
            SleepIntent.ClearNoticesConfirmed -> runNoticeRequest { clearNotices() }

            // 到達した瞬間の 1 回だけ復帰させる。指がさらに動いても重ねて送らない。
            is SleepIntent.UnlockDragged ->
                if (!previous.isUnlockReached && current.isUnlockReached) {
                    _effect.send(SleepEffect.Unlocked)
                }

            is SleepIntent.NoticeClicked ->
                _effect.send(SleepEffect.NoticeSelected(intent.notice.destination))

            SleepIntent.ClearNoticesClicked,
            SleepIntent.ClearNoticesDismissed,
            SleepIntent.ClearNoticesClicked,
            SleepIntent.ClearNoticesDismissed,
            SleepIntent.UnlockCancelled,
            is SleepIntent.ApiNoticesChanged,
            SleepIntent.NoticesLoadFailed,
            is SleepIntent.DeviceNoticesChanged,
            is SleepIntent.Ticked,
            -> Unit
        }
    }

    /** 成功した結果は NoticeRepository の値の変化として購読側に届くので、ここは失敗だけを戻す。 */
    private suspend fun runNoticeRequest(request: suspend () -> Unit) {
        runCatching { request() }
            .onFailure { onIntent(SleepIntent.NoticesLoadFailed) }
    }

    private companion object {
        const val TICK_INTERVAL_MS = 1_000L
    }
}
