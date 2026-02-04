package com.example.androidsampleapp.domain.usecase

import com.example.androidsampleapp.domain.model.ConnectionState
import com.example.androidsampleapp.domain.repository.DeviceRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

class ObserveConnectionStateUseCase @Inject constructor(
    private val repository: DeviceRepository,
) {
    operator fun invoke(): StateFlow<ConnectionState> = repository.connectionState
}
