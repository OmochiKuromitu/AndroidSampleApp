package com.example.androidsampleapp.data

import com.example.androidsampleapp.config.AppConfig
import com.example.androidsampleapp.core.AppStateHolder
import com.example.androidsampleapp.domain.model.Aircon
import com.example.androidsampleapp.domain.model.AirconMode
import com.example.androidsampleapp.domain.repository.AirconRepository
import com.example.androidsampleapp.model.AirconModeRequest
import com.example.androidsampleapp.model.AirconPowerRequest
import com.example.androidsampleapp.model.AirconStatusResponse
import com.example.androidsampleapp.model.AirconTemperatureRequest
import com.example.androidsampleapp.network.DeviceApi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * エアコンの操作 API を呼ぶ。状態の保持は [AppStateHolder] に任せる。
 *
 * 操作 API は操作後の現在値を返すので、次の状態取得を待たずにそれを反映する。
 * 失敗したら例外がそのまま上がり、状態は変えない。
 */
@Singleton
class AirconRepositoryImpl @Inject constructor(
    private val api: DeviceApi,
    private val appStateHolder: AppStateHolder,
    private val config: AppConfig,
) : AirconRepository {

    override val aircon = appStateHolder.aircon

    override suspend fun setPower(isOn: Boolean) {
        apply(api.setAirconPower(AirconPowerRequest(deviceId = config.deviceId, isOn = isOn)))
    }

    override suspend fun setMode(mode: AirconMode) {
        apply(api.setAirconMode(AirconModeRequest(deviceId = config.deviceId, mode = mode.toCode())))
    }

    override suspend fun setTargetTemperature(value: Double) {
        apply(
            api.setAirconTemperature(
                AirconTemperatureRequest(deviceId = config.deviceId, targetTemperature = value),
            ),
        )
    }

    private fun apply(response: AirconStatusResponse) {
        appStateHolder.updateAircon { response.toDomain() }
    }
}

/** 状態取得 API と操作 API で同じ形が返るので、変換はここに 1 つだけ持つ。 */
internal fun AirconStatusResponse.toDomain(): Aircon = Aircon(
    isOn = isOn,
    mode = airconModeOf(mode),
    targetTemperature = targetTemperature,
    roomTemperature = roomTemperature,
)
