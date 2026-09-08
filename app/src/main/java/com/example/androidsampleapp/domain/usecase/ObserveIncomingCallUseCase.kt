package com.example.androidsampleapp.domain.usecase

import com.example.androidsampleapp.domain.model.IncomingCall
import com.example.androidsampleapp.domain.repository.DeviceRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

class ObserveIncomingCallUseCase @Inject constructor(
    private val repository: DeviceRepository,
) {
    operator fun invoke(): StateFlow<IncomingCall?> = repository.incomingCall
}
