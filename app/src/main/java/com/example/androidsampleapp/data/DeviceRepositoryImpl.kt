package com.example.androidsampleapp.data

import com.example.androidsampleapp.config.AppConfig
import com.example.androidsampleapp.core.AppStateHolder
import com.example.androidsampleapp.domain.model.AirconMode
import com.example.androidsampleapp.domain.model.ConnectionState
import com.example.androidsampleapp.domain.model.IncomingCall
import com.example.androidsampleapp.domain.model.Notice
import com.example.androidsampleapp.domain.model.NoticeCategory
import com.example.androidsampleapp.domain.model.NoticeDestination
import com.example.androidsampleapp.domain.repository.DeviceRepository
import com.example.androidsampleapp.model.CommandRequest
import com.example.androidsampleapp.model.DeviceMessage
import com.example.androidsampleapp.network.MessageParser
import com.example.androidsampleapp.network.TcpClient
import com.example.androidsampleapp.network.UdpCommandClient
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive

/**
 * 受信メッセージを [AppStateHolder] に反映する唯一の場所。
 * エアコンの状態も機器からの通知も同じ TCP 接続で降ってくるため、ここでまとめて適用する。
 */
@Singleton
class DeviceRepositoryImpl @Inject constructor(
    private val tcpClient: TcpClient,
    private val udpCommandClient: UdpCommandClient,
    private val messageParser: MessageParser,
    private val appStateHolder: AppStateHolder,
    private val config: AppConfig,
) : DeviceRepository {

    override val connectionState = appStateHolder.connectionState
    override val incomingCall = appStateHolder.incomingCall
    override val deviceNotices = appStateHolder.deviceNotices

    override suspend fun monitor() {
        while (currentCoroutineContext().isActive) {
            appStateHolder.updateConnectionState(ConnectionState.CONNECTING)
            try {
                events().collect(::handleEvent)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // 切断・接続失敗はここでは握りつぶし、下の待機を経て再接続する。
            }
            appStateHolder.updateConnectionState(ConnectionState.DISCONNECTED)
            delay(config.reconnectInterval)
        }
    }

    override suspend fun answerCall(roomId: String) {
        send(CommandRequest.AnswerCall(roomId))
        appStateHolder.updateIncomingCall(null)
    }

    override suspend fun rejectCall(roomId: String) {
        send(CommandRequest.RejectCall(roomId))
        appStateHolder.updateIncomingCall(null)
    }

    override fun clearDeviceNotices() {
        appStateHolder.updateDeviceNotices { emptyList() }
    }

    private suspend fun send(command: CommandRequest) {
        if (config.useFakeDevice) return
        udpCommandClient.send(command)
    }

    private fun events(): Flow<TcpClient.Event> =
        if (config.useFakeDevice) fakeEvents() else tcpClient.connect()

    private fun handleEvent(event: TcpClient.Event) {
        when (event) {
            TcpClient.Event.Connected ->
                appStateHolder.updateConnectionState(ConnectionState.CONNECTED)

            is TcpClient.Event.Line -> applyMessage(messageParser.parse(event.value))
        }
    }

    private fun applyMessage(message: DeviceMessage) {
        when (message) {
            is DeviceMessage.CallStarted -> appStateHolder.updateIncomingCall(
                IncomingCall(roomId = message.roomId, displayName = message.displayName),
            )

            DeviceMessage.CallEnded -> appStateHolder.updateIncomingCall(null)

            is DeviceMessage.AirconStatus -> appStateHolder.updateAircon { current ->
                current.copy(
                    isOn = message.isOn,
                    mode = AirconMode.fromCode(message.mode),
                    targetTemperature = message.targetTemperature,
                    roomTemperature = message.roomTemperature,
                )
            }

            // 新しいものを先頭に積み、古いものは捨てる。
            is DeviceMessage.NoticeReceived -> appStateHolder.updateDeviceNotices { current ->
                (listOf(message.toDomain()) + current).take(MAX_DEVICE_NOTICES)
            }

            DeviceMessage.Pong,
            is DeviceMessage.Unknown,
            -> Unit
        }
    }

    /**
     * 機器は id も時刻も送ってこない前提（仮）。受け取った時点の時刻を入れる。
     * id は API の通知と一覧の中で重ならないよう、接頭辞を付けて手元で振る。
     */
    private fun DeviceMessage.NoticeReceived.toDomain(): Notice = Notice(
        id = DEVICE_NOTICE_ID_PREFIX + UUID.randomUUID(),
        category = NoticeCategory.fromCode(category),
        title = title,
        message = message,
        occurredAt = System.currentTimeMillis(),
        destination = NoticeDestination.fromCode(destination),
    )

    /** mock flavor 用。実機が無くても画面の確認ができるようにする。 */
    private fun fakeEvents(): Flow<TcpClient.Event> = flow {
        emit(TcpClient.Event.Connected)
        emit(TcpClient.Event.Line("AIRCON|ON|COOL|26.0|28.4"))
        emit(TcpClient.Event.Line("NOTICE|ALERT|非常ボタン|集会室の非常ボタンが押されました|TOP"))
        delay(FAKE_CALL_DELAY_MS)
        emit(TcpClient.Event.Line("CALL|101|玄関"))
        var pings = 0
        while (currentCoroutineContext().isActive) {
            delay(FAKE_PING_INTERVAL_MS)
            emit(TcpClient.Event.Line("PONG"))
            // 画面を開いたまま一覧が増えるのを確認できるよう、ときどき通知を流す。
            if (++pings % FAKE_NOTICE_EVERY_PINGS == 0) {
                emit(TcpClient.Event.Line("NOTICE|INFO||機器からのテスト通知です（$pings）|TOP"))
            }
        }
    }

    private companion object {
        const val FAKE_CALL_DELAY_MS = 8_000L
        const val FAKE_PING_INTERVAL_MS = 5_000L
        const val FAKE_NOTICE_EVERY_PINGS = 4

        /** 手元に持つ機器からの通知の上限。超えたら古いものから捨てる。 */
        const val MAX_DEVICE_NOTICES = 20
        const val DEVICE_NOTICE_ID_PREFIX = "device-"
    }
}
