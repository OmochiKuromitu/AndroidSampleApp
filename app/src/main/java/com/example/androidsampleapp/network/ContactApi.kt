package com.example.androidsampleapp.network

import com.example.androidsampleapp.model.CallHistoryResponse
import com.example.androidsampleapp.model.ContactResponse
import com.example.androidsampleapp.model.DeviceRequest
import com.example.androidsampleapp.model.MissedCallCountResponse
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 連絡先（電話帳・通話履歴・不在着信）の HTTP API。パスは [com.example.androidsampleapp.config.AppConfig.apiBaseUrl] からの相対。
 *
 * どれも POST で、本文に端末の ID（[DeviceRequest]）を入れる。メソッドが同じなのでパスで分ける。
 * 2xx 以外は `retrofit2.HttpException`、通信できなければ `java.io.IOException` を投げる。
 * 呼び出しは OkHttp のスレッドで行われるので、メインスレッドから呼んでよい。
 *
 * パスと形はサンプル用の仮の仕様。mock flavor では [FakeApiInterceptor] が通信の手前で JSON を返す。
 */
interface ContactApi {

    @POST("contacts/list")
    suspend fun getContacts(@Body body: DeviceRequest): List<ContactResponse>

    /** 新しい順。 */
    @POST("calls/history")
    suspend fun getCallHistories(@Body body: DeviceRequest): List<CallHistoryResponse>

    /** 未確認の不在着信の件数。 */
    @POST("missed-calls/count")
    suspend fun getMissedCallCount(@Body body: DeviceRequest): MissedCallCountResponse

    /** 未確認の印を消す。本文は返らない前提（204）。 */
    @POST("missed-calls/read")
    suspend fun markMissedCallsAsRead(@Body body: DeviceRequest)
}
