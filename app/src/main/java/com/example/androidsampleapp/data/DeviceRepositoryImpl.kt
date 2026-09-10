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
 * エアコンの状態も同じ TCP 接続で降ってくるため、ここでまとめて適用する。
 */
@Singleton
class DeviceRepositoryImpl @Inject constructor(
    private val tcpClient: TcpClient,
    private val udpCommandClient: UdpCommandClient,
    private val messageParser: MessageParser,
    private val appStateHolder: AppStateHolder,
    private val config: AppConfig,
) : DeviceRepository {

    private var noticeSequence = 0L

    override val connectionState = appStateHolder.connectionState
    override val incomingCall = appStateHolder.incomingCall

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

            is DeviceMessage.NoticeReceived -> appStateHolder.addNotice(
                Notice(
                    // 機器側は id を振らないので、受信順に一意な値を作る。
                    id = "notice-${noticeSequence++}",
                    category = NoticeCategory.fromCode(message.category),
                    message = message.message,
                    destination = NoticeDestination.fromCode(message.destination),
                ),
            )

            DeviceMessage.Pong,
            is DeviceMessage.Unknown,
            -> Unit
        }
    }

    /** mock flavor 用。実機が無くても画面の確認ができるようにする。 */
    private fun fakeEvents(): Flow<TcpClient.Event> = flow {
        emit(TcpClient.Event.Connected)
        emit(TcpClient.Event.Line("AIRCON|ON|COOL|26.0|28.4"))
        emit(TcpClient.Event.Line("NOTICE|INFO|システムを起動しました|TOP"))
        emit(TcpClient.Event.Line("NOTICE|AIRCON|リビングの設定温度を 26.0 度に変更しました|AIRCON"))
        emit(TcpClient.Event.Line("NOTICE|ALERT|フィルターの清掃時期です|AIRCON"))
        emit(TcpClient.Event.Line("NOTICE|CALL|玄関からの呼び出しに応答がありませんでした|TOP"))
        delay(FAKE_CALL_DELAY_MS)
        emit(TcpClient.Event.Line("CALL|101|玄関"))
        while (currentCoroutineContext().isActive) {
            delay(FAKE_PING_INTERVAL_MS)
            emit(TcpClient.Event.Line("PONG"))
        }
    }

    private companion object {
        const val FAKE_CALL_DELAY_MS = 8_000L
        const val FAKE_PING_INTERVAL_MS = 5_000L
    }
}
