package com.example.androidsampleapp.domain.usecase

import com.example.androidsampleapp.domain.repository.ContactRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

class ObserveMissedCallCountUseCase @Inject constructor(
    private val repository: ContactRepository,
) {
    operator fun invoke(): StateFlow<Int> = repository.missedCallCount
}
