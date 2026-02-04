package com.example.androidsampleapp.domain.usecase

import com.example.androidsampleapp.domain.model.Aircon
import com.example.androidsampleapp.domain.repository.AirconRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

class ObserveAirconStateUseCase @Inject constructor(
    private val repository: AirconRepository,
) {
    operator fun invoke(): StateFlow<Aircon> = repository.aircon
}
