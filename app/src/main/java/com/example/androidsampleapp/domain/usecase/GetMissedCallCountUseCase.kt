package com.example.androidsampleapp.domain.usecase

import com.example.androidsampleapp.domain.repository.ContactRepository
import javax.inject.Inject

/** 未確認の不在着信の件数を取得 API から取る。 */
class GetMissedCallCountUseCase @Inject constructor(
    private val repository: ContactRepository,
) {
    suspend operator fun invoke(): Int = repository.getMissedCallCount()
}
