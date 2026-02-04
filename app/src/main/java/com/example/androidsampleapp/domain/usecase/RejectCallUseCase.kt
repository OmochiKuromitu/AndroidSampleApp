package com.example.androidsampleapp.domain.usecase

import com.example.androidsampleapp.domain.repository.DeviceRepository
import javax.inject.Inject

class RejectCallUseCase @Inject constructor(
    private val repository: DeviceRepository,
) {
    suspend operator fun invoke(roomId: String) = repository.rejectCall(roomId)
}
