package com.example.androidsampleapp.data

import com.example.androidsampleapp.config.AppConfig
import com.example.androidsampleapp.core.AppStateHolder
import com.example.androidsampleapp.model.AirconModeRequest
import com.example.androidsampleapp.model.AirconPowerRequest
import com.example.androidsampleapp.model.AirconStatusResponse
import com.example.androidsampleapp.model.AirconTemperatureRequest
import com.example.androidsampleapp.model.CallRequest
import com.example.androidsampleapp.model.DeviceRequest
import com.example.androidsampleapp.model.DeviceStatusResponse
import com.example.androidsampleapp.network.DeviceApi
import java.io.IOException
import kotlin.time.Duration.Companion.seconds

internal class AirconTestFixture {
    val holder = AppStateHolder()
    val api = FakeAirconDeviceApi()
    private val config = AppConfig("http://localhost/api", false, "aircon-test", 30.seconds)
    val repository = AirconRepositoryImpl(api, holder, config)
    val deviceRepository = DeviceRepositoryImpl(api, holder, config)
}

/** 設定要求を記録し、機器の現在値・受理する温度・通信失敗を差し替えられる API。 */
internal class FakeAirconDeviceApi : DeviceApi {
    var status = AirconStatusResponse(true, "HEAT", 22.5, 20.0)
    val temperatures = mutableListOf<AirconTemperatureRequest>()
    val modes = mutableListOf<AirconModeRequest>()
    val powers = mutableListOf<AirconPowerRequest>()
    var acceptedTemperature: Double? = null
    var failCommands = false
    var beforeResponse: suspend () -> Unit = {}

    override suspend fun getStatus(body: DeviceRequest) = DeviceStatusResponse(status)

    override suspend fun setAirconPower(body: AirconPowerRequest): AirconStatusResponse {
        powers += body
        checkCommand()
        status = status.copy(isOn = body.isOn)
        return status
    }

    override suspend fun setAirconMode(body: AirconModeRequest): AirconStatusResponse {
        modes += body
        checkCommand()
        status = status.copy(mode = body.mode)
        return status
    }

    override suspend fun setAirconTemperature(body: AirconTemperatureRequest): AirconStatusResponse {
        temperatures += body
        checkCommand()
        status = status.copy(targetTemperature = acceptedTemperature ?: body.targetTemperature)
        return status
    }

    private suspend fun checkCommand() {
        beforeResponse()
        if (failCommands) throw IOException("command failed")
    }

    override suspend fun answerCall(body: CallRequest) = Unit
    override suspend fun rejectCall(body: CallRequest) = Unit
}
