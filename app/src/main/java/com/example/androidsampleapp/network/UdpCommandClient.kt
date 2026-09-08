package com.example.androidsampleapp.network

import com.example.androidsampleapp.config.AppConfig
import com.example.androidsampleapp.model.CommandRequest
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 機器へのコマンド送信。UDP なので到達確認はしない。
 * 結果は TCP 側の状態通知で確認する前提。
 */
@Singleton
class UdpCommandClient @Inject constructor(
    private val config: AppConfig,
) {
    suspend fun send(command: CommandRequest) = withContext(Dispatchers.IO) {
        val payload = command.encode().toByteArray()
        DatagramSocket().use { socket ->
            socket.send(
                DatagramPacket(
                    payload,
                    payload.size,
                    InetAddress.getByName(config.deviceHost),
                    config.udpPort,
                ),
            )
        }
    }
}
