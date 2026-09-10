package com.example.androidsampleapp.ui.navigation

import com.example.androidsampleapp.config.AppConfig
import com.example.androidsampleapp.core.AppStateHolder
import com.example.androidsampleapp.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * スリープに入るかどうかの判断。きっかけは 3 つ。
 *
 * - 無操作が [AppConfig.sleepTimeout] だけ続いた
 * - アプリがバックグラウンドに移った（[onEnteredBackground]）
 * - 電源ボタンなどで画面が消えた（[onScreenOff]）
 *
 * 「何が起きたらスリープか」をこのクラスの API に並べておき、
 * 呼び出し側（App）は起きた出来事を伝えるだけにする。
 *
 * 画面遷移そのものは行わない。「スリープに入った」という状態だけを更新し、
 * それを見た [AppNavigation] が遷移する。判断と遷移を分けておくと、
 * スリープの条件を増やしたときに navigation を触らずに済む。
 */
@Singleton
class IdleTimer @Inject constructor(
    private val appStateHolder: AppStateHolder,
    private val config: AppConfig,
    @ApplicationScope scope: CoroutineScope,
) {

    /** 操作のたびに増える。値そのものに意味はなく、タイマーの再起動の合図。 */
    private val interactions = MutableStateFlow(0L)

    init {
        scope.launch {
            interactions.collectLatest {
                appStateHolder.updateSleeping(false)
                delay(config.sleepTimeout)
                appStateHolder.updateSleeping(true)
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
        if (appStateHolder.isSleeping.value) return
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

    /**
     * 復帰したときではなく離れたときに倒すのは、復帰時に判定すると遷移が走るまでの
     * 1 フレームだけ前の画面が見えることがあるため。離れる時点で倒しておけば、
     * 戻ってきた最初の描画がスリープ画面になる。
     */
    private fun sleep() {
        appStateHolder.updateSleeping(true)
    }
}
