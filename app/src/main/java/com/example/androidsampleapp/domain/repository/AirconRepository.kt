package com.example.androidsampleapp.domain.repository

import com.example.androidsampleapp.domain.model.AirconMode
import com.example.androidsampleapp.domain.model.Aircon
import kotlinx.coroutines.flow.StateFlow

interface AirconRepository {
    val aircon: StateFlow<Aircon>

    suspend fun setPower(isOn: Boolean)
    suspend fun setMode(mode: AirconMode)
    suspend fun setTargetTemperature(value: Double)
}
