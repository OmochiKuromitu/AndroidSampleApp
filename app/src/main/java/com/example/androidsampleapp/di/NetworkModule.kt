package com.example.androidsampleapp.di

import com.example.androidsampleapp.config.AppConfig
import com.example.androidsampleapp.network.ContactApi
import com.example.androidsampleapp.network.DeviceApi
import com.example.androidsampleapp.network.FakeApiInterceptor
import com.example.androidsampleapp.network.NoticeApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * HTTP API の組み立て。接続先は [AppConfig.apiBaseUrl]、擬似 API にするかは [AppConfig.useFakeApi]。
 * アプリの通信はすべてここで作る Retrofit を通る。
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val TIMEOUT_SECONDS = 10L

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        // サーバが項目を足しても落ちないようにする。
        ignoreUnknownKeys = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(config: AppConfig): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .apply { if (config.useFakeApi) addInterceptor(FakeApiInterceptor()) }
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(config: AppConfig, client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            // Retrofit はベース URL の末尾が `/` でないと受け付けない。設定側は付けない約束。
            .baseUrl(config.apiBaseUrl.trimEnd('/') + "/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideNoticeApi(retrofit: Retrofit): NoticeApi = retrofit.create(NoticeApi::class.java)

    @Provides
    @Singleton
    fun provideDeviceApi(retrofit: Retrofit): DeviceApi = retrofit.create(DeviceApi::class.java)

    @Provides
    @Singleton
    fun provideContactApi(retrofit: Retrofit): ContactApi = retrofit.create(ContactApi::class.java)
}
