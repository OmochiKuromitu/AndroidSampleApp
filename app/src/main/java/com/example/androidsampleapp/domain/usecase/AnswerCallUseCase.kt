package com.example.androidsampleapp.domain.usecase

import com.example.androidsampleapp.domain.repository.DeviceRepository
import javax.inject.Inject

class AnswerCallUseCase @Inject constructor(
    private val repository: DeviceRepository,
) {
    suspend operator fun invoke(roomId: String) = repository.answerCall(roomId)
}
