package com.example.androidsampleapp.core

import com.example.androidsampleapp.di.ApplicationScope
import com.example.androidsampleapp.domain.usecase.GetMissedCallCountUseCase
import com.example.androidsampleapp.domain.usecase.MarkMissedCallsAsReadUseCase
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 不在着信の件数を保持し、取得のタイミングを一手に引き受ける。
 *
 * 見るのは下部バーの連絡先タブ、連絡先画面の履歴タブ、の 2 か所。
 * 読み手が複数いるので共有の器を置く価値がある（1 か所なら画面の State に持たせる）。
 *
 * HTTP なので黙っていても届かない。取りに行くきっかけは [refresh] の呼び出し側
 * （[com.example.androidsampleapp.ui.navigation.AppNavigation]）が決める。
 * タイマーで叩き続けないのは、画面が切り替わる瞬間に取れば十分だから。
 *
 * 既読は [markAsRead]。履歴を見せた時点で連絡先画面が呼ぶ。
 *
 * ViewModel ではなく @Singleton なのは、画面をまたいで同じ件数を見せるため。
 * 保持は Hilt の SingletonComponent が行う（Application と同じ寿命）。
 */
@Singleton
class MissedCallManager @Inject constructor(
    private val getMissedCallCount: GetMissedCallCountUseCase,
    private val markMissedCallsAsRead: MarkMissedCallsAsReadUseCase,
    @ApplicationScope private val scope: CoroutineScope,
) {

    private val _missedCallCount = MutableStateFlow(0)
    val missedCallCount: StateFlow<Int> = _missedCallCount.asStateFlow()

    private var inFlight: Job? = null

    /**
     * 取りに行く。
     *
     * 実行中の要求があれば何もしない。タブを続けて叩かれたときに、同じ要求が
     * 重なって遅い順に上書きされるのを防ぐ。
     * 失敗しても前回の件数を残す。通信が一度こけただけでバッジが消えると、
     * 不在着信を見落とす方に倒れるため。
     */
    fun refresh() {
        if (inFlight?.isActive == true) return
        inFlight = scope.launch {
            runCatching { getMissedCallCount() }
                .onSuccess { _missedCallCount.value = it }
        }
    }

    /**
     * 既読にする。履歴を見せた時点で呼ぶ。
     *
     * [refresh] と違って取りやめない。利用者の操作の結果なので、落とすと
     * 見たのにバッジが残る。実行中の取得があれば打ち切る。既読のあとに取り直すので、
     * 古い件数で上書きされるのを防ぐため。
     */
    fun markAsRead() {
        inFlight?.cancel()
        inFlight = scope.launch {
            runCatching { markMissedCallsAsRead() }
                .onSuccess { _missedCallCount.value = it }
        }
    }
}
