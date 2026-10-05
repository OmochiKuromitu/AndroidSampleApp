package com.example.androidsampleapp.data

import com.example.androidsampleapp.config.AppConfig
import com.example.androidsampleapp.core.MissedCallManager
import com.example.androidsampleapp.domain.model.Notice
import com.example.androidsampleapp.domain.model.NoticeSnapshot
import com.example.androidsampleapp.domain.repository.NoticeRepository
import com.example.androidsampleapp.model.DeviceRequest
import com.example.androidsampleapp.model.NoticeResponse
import com.example.androidsampleapp.network.NoticeApi
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 通知は HTTP の API（[NoticeApi]）から取る。定期的に取る機器の状態とは別の API なので、
 * AppStateHolder は通らない。
 *
 * 結果と取得状況は MissedCallManager に反映し、[noticeSnapshot] として公開する。
 * 一覧の並び替え、失敗時の前回値の保持、取得と消去の競合制御もここで行う。
 * コルーチンは作らず、呼び出し元の寿命で実行する。
 *
 * API はどの端末からの要求かを本文で名乗らせる。端末の ID は [AppConfig.deviceId] から詰める。
 * ID が要るのは通信の都合なので、ここで閉じる。[NoticeRepository] の口は変えず、呼ぶ側は ID を知らない。
 *
 * mock flavor では OkHttp に挟んだ FakeApiInterceptor が JSON を返すので、
 * このクラスはサーバの有無を知らない。
 */
@Singleton
class NoticeRepositoryImpl @Inject constructor(
    private val api: NoticeApi,
    private val config: AppConfig,
    private val missedCallManager: MissedCallManager,
) : NoticeRepository {

    override val noticeSnapshot = missedCallManager.noticeSnapshot
    private val noticeMutex = Mutex()

    override suspend fun refreshNotices() {
        if (!noticeMutex.tryLock()) return
        try {
            requestNotices()
        } finally {
            noticeMutex.unlock()
        }
    }

    override suspend fun deleteAllNotices() {
        noticeMutex.withLock {
            // 消去中に届いた通知も反映する。消去と再取得の間に別の要求を入れない。
            requestNotices { api.deleteAllNotices(request()) }
        }
    }

    private suspend fun requestNotices(beforeFetch: suspend () -> Unit = {}) {
        val previousFailure = noticeSnapshot.value.loadFailed
        missedCallManager.updateNoticeSnapshot { it.copy(loadFailed = false) }
        try {
            beforeFetch()
            val notices = api.getNotices(request())
                .map { it.toDomain() }
                .sortedByDescending { it.occurredAt }
            currentCoroutineContext().ensureActive()
            missedCallManager.updateNoticeSnapshot {
                NoticeSnapshot(notices = notices, isLoaded = true)
            }
        } catch (e: CancellationException) {
            // キャンセルは取得失敗にしない。要求前の失敗状況に戻す。
            missedCallManager.updateNoticeSnapshot { it.copy(loadFailed = previousFailure) }
            throw e
        } catch (e: Exception) {
            missedCallManager.updateNoticeSnapshot { it.copy(loadFailed = true) }
            throw e
        }
    }

    private fun request(): DeviceRequest = DeviceRequest(deviceId = config.deviceId)

    private fun NoticeResponse.toDomain(): Notice = Notice(
        id = id,
        category = noticeCategoryOf(category),
        title = title,
        message = message,
        occurredAt = occurredAt,
        destination = noticeDestinationOf(destination),
        isPinned = pinned,
    )
}
