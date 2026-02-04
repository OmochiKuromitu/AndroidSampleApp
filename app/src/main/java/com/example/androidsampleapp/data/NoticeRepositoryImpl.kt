package com.example.androidsampleapp.data

import com.example.androidsampleapp.config.AppConfig
import com.example.androidsampleapp.domain.model.Notice
import com.example.androidsampleapp.domain.repository.NoticeRepository
import com.example.androidsampleapp.model.DeviceRequest
import com.example.androidsampleapp.model.NoticeResponse
import com.example.androidsampleapp.network.NoticeApi
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 通知は HTTP の API（[NoticeApi]）から取る。定期的に取る機器の状態とは別の API なので、
 * AppStateHolder は通らない。
 *
 * 取った結果は [notices] に持つ。@Singleton なので、スリープに入り直しても前回の一覧がすぐ出て、
 * 取り直しが終われば差し替わる。
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
) : NoticeRepository {

    private val _notices = MutableStateFlow<List<Notice>?>(null)
    override val notices: StateFlow<List<Notice>?> = _notices.asStateFlow()

    override suspend fun refreshNotices() {
        // 失敗したら例外がそのまま上がり、[notices] は前回の値のまま残る。
        _notices.value = api.getNotices(request()).map { it.toDomain() }
    }

    override suspend fun deleteAllNotices() {
        api.deleteAllNotices(request())
    }

    private fun request(): DeviceRequest = DeviceRequest(deviceId = config.deviceId)

    private fun NoticeResponse.toDomain(): Notice = Notice(
        id = id,
        category = noticeCategoryOf(category),
        title = title,
        message = message,
        occurredAt = occurredAt,
        destination = noticeDestinationOf(destination),
    )
}
