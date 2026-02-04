package com.example.androidsampleapp.domain.usecase

import com.example.androidsampleapp.domain.repository.AirconRepository
import javax.inject.Inject

class SetAirconPowerUseCase @Inject constructor(
    private val repository: AirconRepository,
) {
    suspend operator fun invoke(isOn: Boolean) = repository.setPower(isOn)
}
