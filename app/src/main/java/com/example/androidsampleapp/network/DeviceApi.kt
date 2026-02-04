package com.example.androidsampleapp.network

import com.example.androidsampleapp.model.AirconModeRequest
import com.example.androidsampleapp.model.AirconPowerRequest
import com.example.androidsampleapp.model.AirconStatusResponse
import com.example.androidsampleapp.model.AirconTemperatureRequest
import com.example.androidsampleapp.model.CallRequest
import com.example.androidsampleapp.model.DeviceRequest
import com.example.androidsampleapp.model.DeviceStatusResponse
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 機器（エアコンと呼び出し）の HTTP API。パスは [com.example.androidsampleapp.config.AppConfig.apiBaseUrl] からの相対。
 *
 * どれも POST で、本文で端末の ID を名乗る。
 * 2xx 以外は `retrofit2.HttpException`、通信できなければ `java.io.IOException` を投げる。
 * 呼び出しは OkHttp のスレッドで行われるので、メインスレッドから呼んでよい。
 *
 * パスと形はサンプル用の仮の仕様。mock flavor では [FakeApiInterceptor] が通信の手前で JSON を返す。
 */
interface DeviceApi {

    /** 接続状態の確認を兼ねて、着信とエアコンの現在値を取る。定期的に呼ぶ。 */
    @POST("device/status")
    suspend fun getStatus(@Body body: DeviceRequest): DeviceStatusResponse

    /** エアコンの操作。どれも操作後の現在値を返す。 */
    @POST("aircon/power")
    suspend fun setAirconPower(@Body body: AirconPowerRequest): AirconStatusResponse

    @POST("aircon/mode")
    suspend fun setAirconMode(@Body body: AirconModeRequest): AirconStatusResponse

    @POST("aircon/temperature")
    suspend fun setAirconTemperature(@Body body: AirconTemperatureRequest): AirconStatusResponse

    /** 着信への応答と拒否。本文は返らない前提（204）。 */
    @POST("calls/answer")
    suspend fun answerCall(@Body body: CallRequest)

    @POST("calls/reject")
    suspend fun rejectCall(@Body body: CallRequest)
}
