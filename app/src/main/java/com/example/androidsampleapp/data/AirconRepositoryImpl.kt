package com.example.androidsampleapp.data

import com.example.androidsampleapp.config.AppConfig
import com.example.androidsampleapp.core.AppStateHolder
import com.example.androidsampleapp.domain.model.AirconMode
import com.example.androidsampleapp.domain.model.Aircon
import com.example.androidsampleapp.domain.repository.AirconRepository
import com.example.androidsampleapp.model.CommandRequest
import com.example.androidsampleapp.network.UdpCommandClient
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 状態の保持は [AppStateHolder] に任せ、ここは UDP コマンドの送信だけを行う。
 *
 * 実機接続時は、送信後に機器から返る AIRCON 通知で状態が更新される。
 * mock flavor では通知が来ないので、送ったつもりの値をそのまま反映する。
 */
@Singleton
class AirconRepositoryImpl @Inject constructor(
    private val udpCommandClient: UdpCommandClient,
    private val appStateHolder: AppStateHolder,
    private val config: AppConfig,
) : AirconRepository {

    override val aircon = appStateHolder.aircon

    override suspend fun setPower(isOn: Boolean) {
        send(CommandRequest.SetAirconPower(isOn)) { it.copy(isOn = isOn) }
    }

    override suspend fun setMode(mode: AirconMode) {
        send(CommandRequest.SetAirconMode(mode.toCode())) { it.copy(mode = mode) }
    }

    override suspend fun setTargetTemperature(value: Double) {
        send(CommandRequest.SetAirconTemperature(value)) { it.copy(targetTemperature = value) }
    }

    private suspend fun send(
        command: CommandRequest,
        fakeResponse: (Aircon) -> Aircon,
    ) {
        if (config.useFakeDevice) {
            appStateHolder.updateAircon(fakeResponse)
            return
        }
        udpCommandClient.send(command)
    }
}
