package com.example.androidsampleapp.domain.usecase

import com.example.androidsampleapp.domain.model.Notice
import com.example.androidsampleapp.domain.repository.NoticeRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

class ObserveNoticesUseCase @Inject constructor(
    private val repository: NoticeRepository,
) {
    operator fun invoke(): StateFlow<List<Notice>> = repository.notices
}
