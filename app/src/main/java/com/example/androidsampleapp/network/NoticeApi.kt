package com.example.androidsampleapp.network

import com.example.androidsampleapp.model.DeviceRequest
import com.example.androidsampleapp.model.NoticeResponse
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 通知の HTTP API。パスは [com.example.androidsampleapp.config.AppConfig.apiBaseUrl] からの相対。
 *
 * どちらも POST で、本文に端末の ID（[DeviceRequest]）を入れる。メソッドが同じなのでパスで分ける。
 * どちらも 2xx 以外は `retrofit2.HttpException`、通信できなければ `java.io.IOException` を投げる。
 * 呼び出しは OkHttp のスレッドで行われるので、メインスレッドから呼んでよい。
 *
 * パスと形はサンプル用の仮の仕様。mock flavor では [FakeApiInterceptor] が通信の手前で JSON を返す。
 */
interface NoticeApi {

    @POST("notices/list")
    suspend fun getNotices(@Body body: DeviceRequest): List<NoticeResponse>

    /** サーバ側の一覧を空にする。本文は返らない前提（204）。 */
    @POST("notices/delete")
    suspend fun deleteAllNotices(@Body body: DeviceRequest)
}
