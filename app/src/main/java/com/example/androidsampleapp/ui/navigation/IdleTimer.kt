package com.example.androidsampleapp.ui.navigation

import com.example.androidsampleapp.config.AppConfig
import com.example.androidsampleapp.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * スリープ状態の保持と、そこに入るかどうかの判断。きっかけは 4 つ。
 *
 * - 無操作が [AppConfig.sleepTimeout] だけ続いた
 * - アプリがバックグラウンドに移った（[onEnteredBackground]）
 * - 電源ボタンなどで画面が消えた（[onScreenOff]）
 * - スリープタブが選ばれた（[onSleepRequested]）
 *
 * 状態を [com.example.androidsampleapp.core.AppStateHolder] ではなくここに置くのは、
 * 書き手がこのクラスだけで、読み手も [AppNavigation] だけだから。
 * AppStateHolder は機器から降ってくる状態を持つ場所として役割を分けている。
 *
 * 画面遷移そのものは行わない。[isSleeping] を見た [AppNavigation] が遷移する。
 * 判断と遷移を分けておくと、きっかけを増やしたときに navigation を触らずに済む。
 */
@Singleton
class IdleTimer @Inject constructor(
    private val config: AppConfig,
    @ApplicationScope scope: CoroutineScope,
) {

    private val _isSleeping = MutableStateFlow(false)
    val isSleeping: StateFlow<Boolean> = _isSleeping.asStateFlow()

    /** 操作のたびに増える。値そのものに意味はなく、タイマーの再起動の合図。 */
    private val interactions = MutableStateFlow(0L)

    init {
        scope.launch {
            interactions.collectLatest {
                _isSleeping.value = false
                delay(config.sleepTimeout)
                _isSleeping.value = true
            }
        }
    }

    /**
     * 画面が触られた。無操作タイマーを測り直す。
     *
     * スリープ中は無視する。触れただけで解除されると、スリープ画面側で
     * 解除操作（下からのスワイプ）を定義しても意味がなくなるため。
     * 解除は [wake] を使う。
     */
    fun onInteraction() {
        if (_isSleeping.value) return
        interactions.update { it + 1 }
    }

    /** スリープを解除する。解除操作や着信など、明示的に起こしたいときに呼ぶ。 */
    fun wake() {
        interactions.update { it + 1 }
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
        _isSleeping.value = true
    }
}
