package com.example.androidsampleapp.domain.repository

import com.example.androidsampleapp.domain.model.AirconMode
import com.example.androidsampleapp.domain.model.Aircon
import com.example.androidsampleapp.domain.model.AirconSettings
import com.example.androidsampleapp.domain.model.ApiResult
import kotlinx.coroutines.flow.StateFlow

interface AirconRepository {
    val aircon: StateFlow<Aircon>

    /** 最後に設定に成功した値。未設定の項目はデフォルト。プロセス終了で消える。 */
    val settings: StateFlow<AirconSettings>

    /** 操作 API はどれも、成功したら操作後の現在値を反映して [ApiResult.Success] を返す。失敗では何も変えない。 */
    suspend fun setPower(isOn: Boolean): ApiResult<Unit>
    suspend fun setMode(mode: AirconMode): ApiResult<Unit>
    suspend fun setTargetTemperature(value: Double): ApiResult<Unit>
}
