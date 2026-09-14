package com.example.androidsampleapp.core

import com.example.androidsampleapp.di.ApplicationScope
import com.example.androidsampleapp.domain.model.Notice
import com.example.androidsampleapp.domain.usecase.ClearNoticesUseCase
import com.example.androidsampleapp.domain.usecase.GetNoticesUseCase
import com.example.androidsampleapp.domain.usecase.ObserveDeviceNoticesUseCase
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 通知一覧の今の姿。画面はこれをそのまま描く。 */
data class NoticeSnapshot(
    /** API と機器の両方を合わせ、新しい順に並べたもの。 */
    val notices: List<Notice> = emptyList(),
    val isLoading: Boolean = false,
    /** 直近の取得が失敗したか。失敗しても前回の一覧は [notices] に残る。 */
    val loadFailed: Boolean = false,
)

/**
 * 通知一覧を保持し、HTTP の取得のタイミングを一手に引き受ける。
 *
 * 見るのはスリープ画面と連絡先画面の 2 か所。読み手が複数いるので共有の器を置く
 * （1 か所なら画面の State に持たせる）。
 *
 * 通知の出どころは 2 つあり、ここで合わせる。
 * - HTTP の API。黙っていても届かないので、取りに行くきっかけは [refresh] の呼び出し側
 *   （[com.example.androidsampleapp.ui.navigation.AppNavigation]）が決める。
 * - 機器から TCP で届くもの。持ち主は [AppStateHolder] で、ここは購読するだけ。
 *
 * 合わせる場所をここにしたのは、読み手ごとに合わせ方を書くとずれるため。
 * 取り込みの経路は別々のままで、混ぜるのは見せる直前だけ。
 *
 * ViewModel ではなく @Singleton なのは、画面をまたいで同じ一覧を見せるため。
 * [MissedCallManager] と同じ形。
 */
@Singleton
class NoticeManager @Inject constructor(
    private val getNotices: GetNoticesUseCase,
    private val clearNotices: ClearNoticesUseCase,
    observeDeviceNotices: ObserveDeviceNoticesUseCase,
    @ApplicationScope private val scope: CoroutineScope,
) {

    /** API 側の結果。機器側と分けて持つのは、取り直したときに機器からの分を消さないため。 */
    private val api = MutableStateFlow(NoticeSnapshot())

    val snapshot: StateFlow<NoticeSnapshot> =
        combine(api, observeDeviceNotices()) { api, device ->
            api.copy(notices = (api.notices + device).sortedByDescending { it.occurredAt })
        }.stateIn(scope, SharingStarted.Eagerly, NoticeSnapshot())

    private var inFlight: Job? = null

    /**
     * API から取り直す。
     *
     * 実行中の要求があれば何もしない。画面の切り替えが続いたときに、同じ要求が
     * 重なって遅い順に上書きされるのを防ぐ。
     */
    fun refresh() {
        if (inFlight?.isActive == true) return
        inFlight = load { getNotices() }
    }

    /**
     * API の通知と機器から届いた通知を消し、取り直す。
     *
     * [refresh] と違って取りやめない。利用者の操作の結果なので、落とすと消したのに残る。
     * 実行中の取得があれば打ち切る。消す前の一覧で上書きされるのを防ぐため。
     */
    fun clear() {
        inFlight?.cancel()
        inFlight = load { clearNotices() }
    }

    private fun load(fetch: suspend () -> List<Notice>): Job = scope.launch {
        api.update { it.copy(isLoading = true, loadFailed = false) }
        try {
            api.value = NoticeSnapshot(notices = fetch())
        } catch (e: CancellationException) {
            // 打ち切りは失敗ではない。後から始めた方が状態を書く。
            throw e
        } catch (e: Exception) {
            // 一度こけただけで一覧を消さない。前回の一覧を残して失敗だけ立てる。
            api.update { it.copy(isLoading = false, loadFailed = true) }
        }
    }
}
