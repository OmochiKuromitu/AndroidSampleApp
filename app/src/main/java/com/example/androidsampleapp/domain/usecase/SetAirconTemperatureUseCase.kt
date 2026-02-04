package com.example.androidsampleapp.domain.usecase

import com.example.androidsampleapp.domain.model.AirconSpec
import com.example.androidsampleapp.domain.repository.AirconRepository
import javax.inject.Inject

class SetAirconTemperatureUseCase @Inject constructor(
    private val repository: AirconRepository,
) {
    /** 上下限はエアコンの仕様なので、画面ではなくここで丸める。 */
    suspend operator fun invoke(value: Double) =
        repository.setTargetTemperature(AirconSpec.clampTemperature(value))
}
