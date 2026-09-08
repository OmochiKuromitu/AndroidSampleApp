package com.example.androidsampleapp.network

import com.example.androidsampleapp.config.AppConfig
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

/**
 * 機器との TCP 接続。1 行 = 1 メッセージのテキストプロトコルを想定している。
 * 再接続の判断はここではなく data 層（DeviceRepositoryImpl）が持つ。
 */
@Singleton
class TcpClient @Inject constructor(
    private val config: AppConfig,
) {

    sealed interface Event {
        data object Connected : Event
        data class Line(val value: String) : Event
    }

    /**
     * 接続してから切断されるまでを 1 本の Flow で表す。
     * 切断・失敗は例外として上がるので、収集側で握る。
     */
    fun connect(): Flow<Event> = flow {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(config.deviceHost, config.tcpPort), CONNECT_TIMEOUT_MS)
            // readLine() は割り込めないので、タイムアウトで定期的に制御を戻してキャンセルを見る。
            socket.soTimeout = READ_TIMEOUT_MS
            emit(Event.Connected)

            val reader = socket.getInputStream().bufferedReader()
            while (currentCoroutineContext().isActive) {
                val line = try {
                    reader.readLine() ?: break
                } catch (e: SocketTimeoutException) {
                    continue
                }
                emit(Event.Line(line))
            }
        }
    }.flowOn(Dispatchers.IO)

    private companion object {
        const val CONNECT_TIMEOUT_MS = 5_000
        const val READ_TIMEOUT_MS = 1_000
    }
}
