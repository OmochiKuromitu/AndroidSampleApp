package com.example.androidsampleapp.core

import com.example.androidsampleapp.di.ApplicationScope
import com.example.androidsampleapp.domain.usecase.MissedCallUseCase
import com.example.androidsampleapp.domain.usecase.NoticeUseCase
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * HTTP で取る共有の状態（不在着信の件数と通知一覧）を保持し、取得のタイミングを一手に引き受ける。
 *
 * どちらも複数の画面が見るので共有の器を置く（1 か所なら画面の State に持たせる）。
 * - 不在着信の件数: 下部バーの連絡先タブ、連絡先画面の履歴タブ
 * - 通知一覧: スリープ画面、連絡先画面のお知らせタブ
 *
 * HTTP なので黙っていても届かない。取りに行くきっかけは [refresh] の呼び出し側
 * （[com.example.androidsampleapp.ui.navigation.AppNavigation]）が決める。
 * 件数と一覧は同じ節目（起動直後 / タブ移動 / スリープ画面が前に出た）で取り直すので、
 * [refresh] 1 つで両方を取る。タイマーで叩き続けないのは、画面が切り替わる瞬間に取れば十分だから。
 *
 * 通知一覧の持ち主は NoticeRepository で、ここは [NoticeUseCase] から購読して、新しい順に並べ、
 * 失敗の有無と合わせるだけ。読み手ごとに並べ方を書くとずれるので、ここでまとめて行う。
 *
 * 実行中の要求は種類（件数 / 通知）ごとに持つ。1 つにすると、通知の消去が不在着信の取得を
 * 打ち切る、といった無関係な取りやめが起きる。
 *
 * 通信は [MissedCallUseCase] と [NoticeUseCase] を通し、リポジトリは直接触らない。
 * 呼ぶ順番（消去 → 取り直し、既読 → 取り直し）は UseCase が決め、ここはいつ呼ぶか、
 * 重なった要求をどう扱うか、失敗をどう見せるかだけを決める。
 *
 * ViewModel ではなく @Singleton なのは、画面をまたいで同じ値を見せるため。
 * 保持は Hilt の SingletonComponent が行う（Application と同じ寿命）。
 */
@Singleton
class MissedCallManager @Inject constructor(
    private val missedCallUseCase: MissedCallUseCase,
    private val noticeUseCase: NoticeUseCase,
    @ApplicationScope private val scope: CoroutineScope,
) {

    // ---- 不在着信の件数 ----

    private val _missedCallCount = MutableStateFlow(0)
    val missedCallCount: StateFlow<Int> = _missedCallCount.asStateFlow()

    private var missedCallJob: Job? = null

    // ---- 通知一覧 ----

    /** 直近の要求が失敗したか。成功した結果はリポジトリの値の変化として流れるので、ここは失敗だけを持つ。 */
    private val noticeLoadFailed = MutableStateFlow(false)

    val noticeSnapshot: StateFlow<NoticeSnapshot> =
        combine(noticeUseCase.observe(), noticeLoadFailed) { notices, failed ->
            NoticeSnapshot(
                notices = notices.orEmpty().sortedByDescending { it.occurredAt },
                isLoaded = notices != null,
                loadFailed = failed,
            )
        }.stateIn(scope, SharingStarted.Eagerly, NoticeSnapshot())

    private var noticeJob: Job? = null

    /**
     * 不在着信の件数と通知一覧を取り直す。
     *
     * それぞれ、実行中の要求があれば何もしない。画面の切り替えが続いたときに、同じ要求が
     * 重なって遅い順に上書きされるのを防ぐ。
     */
    fun refresh() {
        refreshMissedCallCount()
        refreshNotices()
    }

    /**
     * 不在着信を既読にする。履歴を見せた時点で呼ぶ。
     *
     * [refresh] と違って取りやめない。利用者の操作の結果なので、落とすと
     * 見たのにバッジが残る。実行中の件数の取得があれば打ち切る。既読のあとに取り直すので、
     * 古い件数で上書きされるのを防ぐため。
     */
    fun markAsRead() {
        missedCallJob?.cancel()
        missedCallJob = scope.launch {
            runCatching { missedCallUseCase.markAsRead() }
                .onSuccess { _missedCallCount.value = it }
        }
    }

    /**
     * 通知を消し、取り直す。
     *
     * [refresh] と違って取りやめない。利用者の操作の結果なので、落とすと消したのに残る。
     * 実行中の通知の取得があれば打ち切る。消す前の一覧で上書きされるのを防ぐため。
     */
    fun clearNotices() {
        noticeJob?.cancel()
        noticeJob = launchNoticeRequest { noticeUseCase.clear() }
    }

    /**
     * 失敗しても前回の件数を残す。通信が一度こけただけでバッジが消えると、
     * 不在着信を見落とす方に倒れるため。
     */
    private fun refreshMissedCallCount() {
        if (missedCallJob?.isActive == true) return
        missedCallJob = scope.launch {
            runCatching { missedCallUseCase.getCount() }
                .onSuccess { _missedCallCount.value = it }
        }
    }

    private fun refreshNotices() {
        if (noticeJob?.isActive == true) return
        noticeJob = launchNoticeRequest { noticeUseCase.refresh() }
    }

    private fun launchNoticeRequest(request: suspend () -> Unit): Job = scope.launch {
        // 取り直しを始めるので、前回の失敗は消す。
        noticeLoadFailed.value = false
        try {
            request()
        } catch (e: CancellationException) {
            // 打ち切りは失敗ではない。後から始めた方が状態を書く。
            throw e
        } catch (e: Exception) {
            // 一度こけただけで一覧を消さない。受け取り済みの一覧を残して失敗だけ立てる。
            noticeLoadFailed.value = true
        }
    }
}
