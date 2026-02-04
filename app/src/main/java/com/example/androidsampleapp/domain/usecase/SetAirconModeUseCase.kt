package com.example.androidsampleapp.domain.usecase

import com.example.androidsampleapp.domain.model.AirconMode
import com.example.androidsampleapp.domain.repository.AirconRepository
import javax.inject.Inject

class SetAirconModeUseCase @Inject constructor(
    private val repository: AirconRepository,
) {
    suspend operator fun invoke(mode: AirconMode) = repository.setMode(mode)
}
