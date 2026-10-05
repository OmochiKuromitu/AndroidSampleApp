package com.example.androidsampleapp.data

import com.example.androidsampleapp.config.AppConfig
import com.example.androidsampleapp.core.AppStateHolder
import com.example.androidsampleapp.domain.model.Aircon
import com.example.androidsampleapp.domain.model.AirconMode
import com.example.androidsampleapp.domain.model.AirconSettings
import com.example.androidsampleapp.domain.repository.AirconRepository
import com.example.androidsampleapp.model.AirconModeRequest
import com.example.androidsampleapp.model.AirconPowerRequest
import com.example.androidsampleapp.model.AirconStatusResponse
import com.example.androidsampleapp.model.AirconTemperatureRequest
import com.example.androidsampleapp.network.DeviceApi
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * エアコンの操作 API を呼ぶ。機器の現在値は [AppStateHolder] に反映する。
 *
 * 操作 API は操作後の現在値を返すので、次の状態取得を待たずにそれを反映する。
 * 失敗したら例外がそのまま上がり、状態は変えない。
 * 温度とモードは、設定に成功した項目だけを [settings] に保存する。
 * Singleton のメモリに保持するので画面を作り直しても残り、プロセス終了で消える。
 * 状態取得や電源操作の応答では、この設定値を上書きしない。
 */
@Singleton
class AirconRepositoryImpl @Inject constructor(
    private val api: DeviceApi,
    private val appStateHolder: AppStateHolder,
    private val config: AppConfig,
) : AirconRepository {

    override val aircon = appStateHolder.aircon

    private val _settings = MutableStateFlow(AirconSettings())
    override val settings: StateFlow<AirconSettings> = _settings.asStateFlow()

    override suspend fun setPower(isOn: Boolean) {
        apply(api.setAirconPower(AirconPowerRequest(deviceId = config.deviceId, isOn = isOn)))
    }

    override suspend fun setMode(mode: AirconMode) {
        val response = api.setAirconMode(AirconModeRequest(deviceId = config.deviceId, mode = mode.toCode()))
        currentCoroutineContext().ensureActive()
        _settings.update { it.copy(mode = airconModeOf(response.mode)) }
        apply(response)
    }

    override suspend fun setTargetTemperature(value: Double) {
        val response = api.setAirconTemperature(
            AirconTemperatureRequest(deviceId = config.deviceId, targetTemperature = value),
        )
        currentCoroutineContext().ensureActive()
        _settings.update { it.copy(targetTemperature = response.targetTemperature) }
        apply(response)
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
