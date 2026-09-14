package com.example.androidsampleapp.data

import com.example.androidsampleapp.domain.model.Notice
import com.example.androidsampleapp.domain.model.NoticeCategory
import com.example.androidsampleapp.domain.model.NoticeDestination
import com.example.androidsampleapp.domain.repository.NoticeRepository
import com.example.androidsampleapp.model.NoticeResponse
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay

/**
 * 通知は HTTP の API から取る。機器との TCP とは経路が別なので、
 * AppStateHolder（機器から降ってくる状態）は通らない。
 *
 * サーバ側が未実装のため、今は [fakeServerNotices] が「サーバが持っているはずのデータ」を
 * 肩代わりしている。API ができたらこのフィールドごと消す。
 */
@Singleton
class NoticeRepositoryImpl @Inject constructor() : NoticeRepository {

    /** TODO: API ができたら削除する。本来はサーバが持つデータ。 */
    private var fakeServerNotices: List<NoticeResponse> = FAKE_RESPONSE

    override suspend fun getNotices(): List<Notice> {
        // TODO: GET {AppConfig.apiBaseUrl}/notices に置き換える。
        delay(API_DELAY_MS)
        return fakeServerNotices.map { it.toDomain() }
    }

    override suspend fun deleteAllNotices() {
        // TODO: DELETE {AppConfig.apiBaseUrl}/notices に置き換える。
        delay(API_DELAY_MS)
        fakeServerNotices = emptyList()
    }

    private fun NoticeResponse.toDomain(): Notice = Notice(
        id = id,
        category = NoticeCategory.fromCode(category),
        title = title,
        message = message,
        occurredAt = occurredAt,
        destination = NoticeDestination.fromCode(destination),
    )

    private companion object {
        /** 通信の遅れを画面で確認できるよう、仮データでも少し待たせている。 */
        const val API_DELAY_MS = 500L

        const val HOUR_MS = 60 * 60 * 1000L

        /** 時刻は起動時点からさかのぼって置く。機器から届く通知と混ぜたときの並びを見るため。 */
        private val now = System.currentTimeMillis()

        val FAKE_RESPONSE = listOf(
            NoticeResponse(
                "1", "CALL", "不在着信", "玄関からの呼び出しに応答がありませんでした",
                now - 1 * HOUR_MS, "CONTACT_MISSED",
            ),
            NoticeResponse(
                "2", "ALERT", "フィルター", "フィルターの清掃時期です",
                now - 3 * HOUR_MS, "AIRCON",
            ),
            NoticeResponse(
                "3", "AIRCON", "リビング", "設定温度を 26.0 度に変更しました",
                now - 12 * HOUR_MS, "AIRCON",
            ),
            NoticeResponse(
                "4", "ALERT", "故障情報：0402", "室外機の通信が途絶えています\n点検を依頼してください",
                now - 20 * HOUR_MS, "AIRCON",
            ),
            NoticeResponse("5", "INFO", null, "システムを起動しました", now - 30 * HOUR_MS, "TOP"),
        )
    }
}
