package com.example.androidsampleapp.data

import com.example.androidsampleapp.domain.model.Notice
import com.example.androidsampleapp.domain.model.NoticeCategory
import com.example.androidsampleapp.domain.model.NoticeDestination
import com.example.androidsampleapp.domain.repository.NoticeRepository
import com.example.androidsampleapp.model.NoticeResponse
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 通知は HTTP の API から取る。機器との TCP とは経路が別なので、
 * AppStateHolder（機器から降ってくる状態）は通らない。
 *
 * サーバ側が未実装のため、今は [fetchFromApi] が仮データを返す。
 * 差し替えるのはこのクラスの中だけで、UseCase から上は変わらない。
 */
@Singleton
class NoticeRepositoryImpl @Inject constructor() : NoticeRepository {

    private val _notices = MutableStateFlow<List<Notice>>(emptyList())
    override val notices: StateFlow<List<Notice>> = _notices.asStateFlow()

    override suspend fun refresh() {
        _notices.value = fetchFromApi().map { it.toDomain() }
    }

    override suspend fun clear() {
        // TODO: API ができたら DELETE {AppConfig.apiBaseUrl}/notices に置き換える。
        _notices.value = emptyList()
    }

    /**
     * TODO: API ができたら GET {AppConfig.apiBaseUrl}/notices に置き換える。
     * 通信の遅れを画面で確認できるよう、仮データでも少し待たせている。
     */
    private suspend fun fetchFromApi(): List<NoticeResponse> {
        delay(FETCH_DELAY_MS)
        return FAKE_RESPONSE
    }

    private fun NoticeResponse.toDomain(): Notice = Notice(
        id = id,
        category = NoticeCategory.fromCode(category),
        message = message,
        destination = NoticeDestination.fromCode(destination),
    )

    private companion object {
        const val FETCH_DELAY_MS = 500L

        /** API 実装までの仮データ。新しいものが先頭。 */
        val FAKE_RESPONSE = listOf(
            NoticeResponse("1", "CALL", "玄関からの呼び出しに応答がありませんでした", "TOP"),
            NoticeResponse("2", "ALERT", "フィルターの清掃時期です", "AIRCON"),
            NoticeResponse("3", "AIRCON", "リビングの設定温度を 26.0 度に変更しました", "AIRCON"),
            NoticeResponse("4", "INFO", "システムを起動しました", "TOP"),
        )
    }
}
