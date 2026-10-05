package com.example.androidsampleapp.domain.repository

import com.example.androidsampleapp.domain.model.AirconMode
import com.example.androidsampleapp.domain.model.Aircon
import com.example.androidsampleapp.domain.model.AirconSettings
import kotlinx.coroutines.flow.StateFlow

interface AirconRepository {
    val aircon: StateFlow<Aircon>

    /** 最後に設定に成功した値。未設定の項目はデフォルト。プロセス終了で消える。 */
    val settings: StateFlow<AirconSettings>

    suspend fun setPower(isOn: Boolean)
    suspend fun setMode(mode: AirconMode)
    suspend fun setTargetTemperature(value: Double)
}
