package com.example.androidsampleapp.ui.sleep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidsampleapp.domain.usecase.ClearNoticesUseCase
import com.example.androidsampleapp.domain.usecase.ObserveNoticesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
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
 * - 通知一覧は UseCase 経由で購読する。共通の取得のきっかけは AppNavigation。
 *   確認ダイアログで消去を選ばれたら、消去の UseCase を呼ぶ。
 * - 解除スワイプが必要な距離に届いた瞬間と、通知がタップされたときに Effect を出す。
 *   どこへ行くかは AppNavigation が決める。
 */
@HiltViewModel
class SleepViewModel @Inject constructor(
    observeNotices: ObserveNoticesUseCase,
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
        // Repository が公開する共有一覧を UseCase 経由で購読する。
        // StateFlow なので、スリープに入る前に取れていた分も購読した時点で流れてくる。
        viewModelScope.launch {
            observeNotices().collect { onIntent(SleepIntent.NoticesChanged(it)) }
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
    }

    /**
     * 状態を変えうる入力の入口。Route からの操作も、購読した値の変化も、すべてここを通す。
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
            // 結果は購読側から NoticesChanged で戻る。
            // 押しただけの ClearNoticesClicked では消さない（確認ダイアログを出すだけ）。
            SleepIntent.ClearNoticesConfirmed -> clear()

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
            is SleepIntent.NoticesChanged,
            is SleepIntent.Ticked,
            -> Unit
        }
    }

    private suspend fun clear() {
        try {
            clearNotices()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // 前回の一覧を残し、消去の失敗は NoticesChanged で受け取る。
        }
    }

    private companion object {
        const val TICK_INTERVAL_MS = 1_000L
    }
}
