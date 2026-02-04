package com.example.androidsampleapp.data

import com.example.androidsampleapp.config.AppConfig
import com.example.androidsampleapp.core.AppStateHolder
import com.example.androidsampleapp.domain.model.ConnectionState
import com.example.androidsampleapp.domain.model.IncomingCall
import com.example.androidsampleapp.domain.repository.DeviceRepository
import com.example.androidsampleapp.model.CallRequest
import com.example.androidsampleapp.model.DeviceRequest
import com.example.androidsampleapp.model.DeviceStatusResponse
import com.example.androidsampleapp.network.DeviceApi
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * 状態取得 API の結果を [AppStateHolder] に反映する。
 *
 * HTTP は黙っていても届かないので、[monitor] が一定間隔（[AppConfig.pollInterval]）で取りに行く。
 * 接続状態は「直近の取得が成功したか」で決める。
 *
 * サーバの有無はこのクラスは知らない。mock flavor では OkHttp に挟んだ FakeApiInterceptor が JSON を返す。
 */
@Singleton
class DeviceRepositoryImpl @Inject constructor(
    private val api: DeviceApi,
    private val appStateHolder: AppStateHolder,
    private val config: AppConfig,
) : DeviceRepository {

    override val connectionState = appStateHolder.connectionState
    override val incomingCall = appStateHolder.incomingCall

    override suspend fun monitor() {
        // 前回の監視で繋がっていたなら、再開直後に「接続中…」へ戻さない。
        if (!connectionState.value.isConnected) {
            appStateHolder.updateConnectionState(ConnectionState.CONNECTING)
        }
        while (currentCoroutineContext().isActive) {
            try {
                applyStatus(api.getStatus(DeviceRequest(deviceId = config.deviceId)))
                appStateHolder.updateConnectionState(ConnectionState.CONNECTED)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // 一度の失敗では止めない。次の周期で取り直す。
                appStateHolder.updateConnectionState(ConnectionState.DISCONNECTED)
            }
            delay(config.pollInterval)
        }
    }

    override suspend fun answerCall(roomId: String) {
        api.answerCall(CallRequest(deviceId = config.deviceId, roomId = roomId))
        appStateHolder.updateIncomingCall(null)
    }

    override suspend fun rejectCall(roomId: String) {
        api.rejectCall(CallRequest(deviceId = config.deviceId, roomId = roomId))
        appStateHolder.updateIncomingCall(null)
    }

    private fun applyStatus(status: DeviceStatusResponse) {
        appStateHolder.updateIncomingCall(
            status.incomingCall?.let { IncomingCall(roomId = it.roomId, displayName = it.displayName) },
        )
        appStateHolder.updateAircon { status.aircon.toDomain() }
    }
}
