package com.example.androidsampleapp.config

import com.example.androidsampleapp.BuildConfig
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * flavor ごとに変わる設定。値の出所は build.gradle.kts の buildConfigField 1 か所に寄せ、
 * アプリ側は BuildConfig を直接触らずこのクラス経由で読む。
 */
data class AppConfig(
    /** HTTP API のベース URL（末尾の `/` は付けない）。 */
    val apiBaseUrl: String,
    /**
     * サーバが無い環境で動かすための擬似 API。mock flavor でのみ true。
     * 通信の手前で OkHttp の Interceptor が JSON を返すので、Retrofit と JSON の解釈は本物を通る。
     */
    val useFakeApi: Boolean,
    /**
     * HTTP API に名乗る端末の ID。各 API の本文に入れる。
     * 固定の設定値なので、接続先と同じく flavor ごとの buildConfigField で持つ。
     */
    val deviceId: String,
    val sleepTimeout: Duration,
    /** 状態取得 API を呼ぶ間隔。着信に気づくまでの遅れはこの長さで決まる。 */
    val pollInterval: Duration = DEFAULT_POLL_INTERVAL,
) {
    companion object {
        private val DEFAULT_POLL_INTERVAL = 3_000.milliseconds

        fun fromBuildConfig(): AppConfig = AppConfig(
            apiBaseUrl = BuildConfig.API_BASE_URL,
            useFakeApi = BuildConfig.USE_FAKE_API,
            deviceId = BuildConfig.DEVICE_ID,
            sleepTimeout = BuildConfig.SLEEP_TIMEOUT_MS.milliseconds,
        )
    }
}
