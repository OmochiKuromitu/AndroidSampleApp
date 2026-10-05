package com.example.androidsampleapp.domain.usecase

import com.example.androidsampleapp.domain.repository.ContactRepository
import javax.inject.Inject

class MarkMissedCallsAsReadUseCase @Inject constructor(
    private val repository: ContactRepository,
) {
    suspend operator fun invoke() = repository.markMissedCallsAsRead()
}
