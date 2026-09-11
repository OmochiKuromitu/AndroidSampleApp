package com.example.androidsampleapp.config

import com.example.androidsampleapp.BuildConfig
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * flavor ごとに変わる設定。値の出所は build.gradle.kts の buildConfigField 1 か所に寄せ、
 * アプリ側は BuildConfig を直接触らずこのクラス経由で読む。
 */
data class AppConfig(
    val deviceHost: String,
    val tcpPort: Int,
    val udpPort: Int,
    /** 通知取得 API のベース URL。サーバ実装待ちのため、まだ実際には叩いていない。 */
    val apiBaseUrl: String,
    /** 実機が無い環境で動かすための擬似デバイス。mock flavor でのみ true。 */
    val useFakeDevice: Boolean,
    val sleepTimeout: Duration,
    val reconnectInterval: Duration = DEFAULT_RECONNECT_INTERVAL,
) {
    companion object {
        private val DEFAULT_RECONNECT_INTERVAL = 3_000.milliseconds

        fun fromBuildConfig(): AppConfig = AppConfig(
            deviceHost = BuildConfig.DEVICE_HOST,
            tcpPort = BuildConfig.TCP_PORT,
            udpPort = BuildConfig.UDP_PORT,
            apiBaseUrl = BuildConfig.API_BASE_URL,
            useFakeDevice = BuildConfig.USE_FAKE_DEVICE,
            sleepTimeout = BuildConfig.SLEEP_TIMEOUT_MS.milliseconds,
        )
    }
}
