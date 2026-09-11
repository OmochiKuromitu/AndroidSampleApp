package com.example.androidsampleapp.ui.navigation

import com.example.androidsampleapp.config.AppConfig
import com.example.androidsampleapp.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * スリープ画面へ入る条件の判断と、スリープ状態の保持。
 *
 * きっかけは 4 つ。
 * - 無操作が [AppConfig.sleepTimeout] だけ続いた（どの画面でも）
 * - アプリがバックグラウンドに移った（[onEnteredBackground]）
 * - 電源ボタンなどで画面が消えた（[onScreenOff]）
 * - スリープタブが選ばれた（[onSleepRequested]）
 *
 * ViewModel ではなく @Singleton なのは、上の 2 つを Application が拾う必要があるため。
 * Application からは ViewModel に触れない。Composable からは [IdleTimerViewModel] が窓口になる。
 *
 * **タイマーと状態は分けてある。** [resetTimer] や [pauseTimer] はタイマーだけを操作し、
 * [isSleeping] は変えない。状態を変えるのは [wake] とスリープのきっかけ 4 つだけ。
 * 混ぜると「タイマーを再開したら勝手に起きた」のような挙動になる。
 *
 * 画面遷移そのものは行わない。[isSleeping] を見た [AppNavigation] が遷移する。
 *
 * 呼び出しはすべてメインスレッドから来る前提（UI のイベントと Application の受信）。
 */
@Singleton
class IdleTimer @Inject constructor(
    private val config: AppConfig,
    @ApplicationScope private val scope: CoroutineScope,
) {

    private val _isSleeping = MutableStateFlow(false)
    val isSleeping: StateFlow<Boolean> = _isSleeping.asStateFlow()

    private var timerJob: Job? = null

    init {
        startTimer()
    }

    /**
     * ユーザー操作を検知したら呼ぶ。無操作タイマーを測り直す。
     *
     * スリープ中は何もしない。触れただけで解除されると、スリープ画面側で
     * 解除操作（下からのスワイプ）を定義しても意味がなくなるため。解除は [wake]。
     */
    fun resetTimer() {
        if (_isSleeping.value) return
        startTimer()
    }

    /** タイマーを止める。状態は変えない。 */
    fun pauseTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    /** [pauseTimer] 後に測り直しから再開する。状態は変えない。 */
    fun resumeTimer() {
        if (timerJob?.isActive != true) startTimer()
    }

    /** スリープを解除して測り直す。解除操作や着信など、明示的に起こしたいときに呼ぶ。 */
    fun wake() {
        _isSleeping.value = false
        startTimer()
    }

    /** アプリがバックグラウンドに移ったときに呼ぶ。 */
    fun onEnteredBackground() = sleep()

    /**
     * 電源ボタンや画面消灯タイムアウトで画面が消えたときに呼ぶ。
     *
     * キオスク（LockTask）では消灯から復帰するとそのままアプリに戻るため、
     * バックグラウンド移行としては扱われないことがある。消灯そのものを
     * きっかけにしておけば、復帰後は必ずスリープ画面から始まる。
     */
    fun onScreenOff() = sleep()

    /** 利用者がスリープタブを選んだときに呼ぶ。 */
    fun onSleepRequested() = sleep()

    /**
     * 復帰したときではなく離れたときに倒すのは、復帰時に判定すると遷移が走るまでの
     * 1 フレームだけ前の画面が見えることがあるため。離れる時点で倒しておけば、
     * 戻ってきた最初の描画がスリープ画面になる。
     */
    private fun sleep() {
        pauseTimer()
        _isSleeping.value = true
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            delay(config.sleepTimeout)
            _isSleeping.value = true
        }
    }
}
