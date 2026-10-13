package com.example.androidsampleapp.domain.usecase

import com.example.androidsampleapp.domain.model.AirconSettings
import com.example.androidsampleapp.domain.repository.AirconRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

class ObserveAirconSettingsUseCase @Inject constructor(
    private val repository: AirconRepository,
) {
    operator fun invoke(): StateFlow<AirconSettings> = repository.settings
}
