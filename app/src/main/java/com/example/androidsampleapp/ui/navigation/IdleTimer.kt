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
 * 無操作の監視。タイムアウトすると [AppStateHolder] のスリープ状態を立てる。
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

    /** 画面が触られた、あるいは復帰させたいときに呼ぶ。 */
    fun onInteraction() {
        interactions.update { it + 1 }
    }
}
