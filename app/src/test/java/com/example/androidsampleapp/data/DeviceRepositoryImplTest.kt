package com.example.androidsampleapp.data

import com.example.androidsampleapp.config.AppConfig
import com.example.androidsampleapp.core.AppStateHolder
import com.example.androidsampleapp.domain.model.ConnectionState
import com.example.androidsampleapp.network.MessageParser
import com.example.androidsampleapp.network.TcpClient
import com.example.androidsampleapp.network.UdpCommandClient
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceRepositoryImplTest {

    private val config = AppConfig(
        deviceHost = "127.0.0.1",
        tcpPort = 0,
        udpPort = 0,
        apiBaseUrl = "",
        useFakeDevice = true,
        sleepTimeout = 30.seconds,
    )

    private fun testNoticeNumbers(holder: AppStateHolder): List<Int> =
        holder.deviceNotices.value
            .mapNotNull { Regex("テスト通知です（(\\d+)）").find(it.message)?.groupValues?.get(1)?.toInt() }

    @Test
    fun `擬似デバイスのテスト通知は 20 件で止まり、接続は切れない`() = runTest {
        val holder = AppStateHolder()
        val repository = DeviceRepositoryImpl(
            tcpClient = TcpClient(config),
            udpCommandClient = UdpCommandClient(config),
            messageParser = MessageParser(),
            appStateHolder = holder,
            config = config,
        )
        backgroundScope.launch { repository.monitor() }

        // 通知は 20 秒ごと（PONG 5 秒 x 4 回）。20 件分を十分に越える時間を進める。
        advanceTimeBy(ELAPSED_MS)
        runCurrent()

        // 一覧は新しいものが先頭。20 件目で止まり、21 件目は来ていない。
        assertEquals(20, testNoticeNumbers(holder).first())
        // 流れを終わらせていないので、繋ぎ直しにも入っていない。
        assertEquals(ConnectionState.CONNECTED, holder.connectionState.value)
    }

    private companion object {
        /** 着信までの 8 秒 + 通知 20 件分の 400 秒を大きく越える、30 件分相当の時間。 */
        const val ELAPSED_MS = 8_000L + 30 * 20_000L
    }
}
