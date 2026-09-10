package com.example.androidsampleapp.domain.usecase

import com.example.androidsampleapp.domain.repository.NoticeRepository
import javax.inject.Inject

class ClearNoticesUseCase @Inject constructor(
    private val repository: NoticeRepository,
) {
    suspend operator fun invoke() = repository.clear()
}
