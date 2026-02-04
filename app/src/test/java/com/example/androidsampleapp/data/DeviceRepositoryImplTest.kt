package com.example.androidsampleapp.data

import com.example.androidsampleapp.config.AppConfig
import com.example.androidsampleapp.core.AppStateHolder
import com.example.androidsampleapp.domain.model.AirconMode
import com.example.androidsampleapp.domain.model.ConnectionState
import com.example.androidsampleapp.domain.model.IncomingCall
import com.example.androidsampleapp.model.AirconModeRequest
import com.example.androidsampleapp.model.AirconPowerRequest
import com.example.androidsampleapp.model.AirconStatusResponse
import com.example.androidsampleapp.model.AirconTemperatureRequest
import com.example.androidsampleapp.model.CallRequest
import com.example.androidsampleapp.model.DeviceRequest
import com.example.androidsampleapp.model.DeviceStatusResponse
import com.example.androidsampleapp.model.IncomingCallResponse
import com.example.androidsampleapp.network.DeviceApi
import java.io.IOException
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceRepositoryImplTest {

    private val config = AppConfig(
        apiBaseUrl = "http://localhost/api",
        useFakeApi = true,
        deviceId = DEVICE_ID,
        sleepTimeout = 30.seconds,
        pollInterval = POLL_INTERVAL,
    )

    /** 状態取得の応答を差し替えられる偽の API。呼ばれた本文を記録する。 */
    private class FakeDeviceApi : DeviceApi {
        var status: () -> DeviceStatusResponse = { STATUS }
        var statusCount = 0
            private set
        val answered = mutableListOf<CallRequest>()

        override suspend fun getStatus(body: DeviceRequest): DeviceStatusResponse {
            statusCount++
            return status()
        }

        override suspend fun setAirconPower(body: AirconPowerRequest) = STATUS.aircon
        override suspend fun setAirconMode(body: AirconModeRequest) = STATUS.aircon
        override suspend fun setAirconTemperature(body: AirconTemperatureRequest) = STATUS.aircon

        override suspend fun answerCall(body: CallRequest) {
            answered += body
        }

        override suspend fun rejectCall(body: CallRequest) = Unit
    }

    @Test
    fun `状態を取れたら接続中になり、着信とエアコンの値が入る`() = runTest {
        val holder = AppStateHolder()
        val repository = DeviceRepositoryImpl(FakeDeviceApi(), holder, config)

        backgroundScope.launch { repository.monitor() }
        runCurrent()

        assertEquals(ConnectionState.CONNECTED, holder.connectionState.value)
        assertEquals(IncomingCall(roomId = "101", displayName = "玄関"), holder.incomingCall.value)
        assertEquals(AirconMode.HEAT, holder.aircon.value.mode)
        assertEquals(22.5, holder.aircon.value.targetTemperature, 0.0)
    }

    @Test
    fun `取れなければ未接続になり、次の周期で取り直す`() = runTest {
        val api = FakeDeviceApi()
        var shouldFail = true
        api.status = { if (shouldFail) throw IOException("通信失敗") else STATUS }
        val holder = AppStateHolder()
        val repository = DeviceRepositoryImpl(api, holder, config)

        backgroundScope.launch { repository.monitor() }
        runCurrent()
        assertEquals(ConnectionState.DISCONNECTED, holder.connectionState.value)

        shouldFail = false
        advanceTimeBy(POLL_INTERVAL)
        runCurrent()

        assertEquals(ConnectionState.CONNECTED, holder.connectionState.value)
        assertEquals(2, api.statusCount)
    }

    @Test
    fun `着信が消えたら着信なしに戻る`() = runTest {
        val api = FakeDeviceApi()
        val holder = AppStateHolder()
        val repository = DeviceRepositoryImpl(api, holder, config)
        backgroundScope.launch { repository.monitor() }
        runCurrent()

        api.status = { STATUS.copy(incomingCall = null) }
        advanceTimeBy(POLL_INTERVAL)
        runCurrent()

        assertNull(holder.incomingCall.value)
    }

    @Test
    fun `応答すると端末の ID と部屋を送り、着信を消す`() = runTest {
        val api = FakeDeviceApi()
        val holder = AppStateHolder()
        holder.updateIncomingCall(IncomingCall(roomId = "101", displayName = "玄関"))
        val repository = DeviceRepositoryImpl(api, holder, config)

        repository.answerCall("101")

        assertEquals(listOf(CallRequest(deviceId = DEVICE_ID, roomId = "101")), api.answered)
        assertNull(holder.incomingCall.value)
    }

    private companion object {
        const val DEVICE_ID = "device-test"
        val POLL_INTERVAL = 3.seconds

        val STATUS = DeviceStatusResponse(
            aircon = AirconStatusResponse(
                isOn = true,
                mode = "HEAT",
                targetTemperature = 22.5,
                roomTemperature = 20.0,
            ),
            incomingCall = IncomingCallResponse(roomId = "101", displayName = "玄関"),
        )
    }
}
