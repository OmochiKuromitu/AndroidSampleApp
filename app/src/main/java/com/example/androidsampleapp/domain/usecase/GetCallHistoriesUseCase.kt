package com.example.androidsampleapp.domain.usecase

import com.example.androidsampleapp.domain.model.CallHistory
import com.example.androidsampleapp.domain.repository.ContactRepository
import javax.inject.Inject

/** 通話履歴を取得 API から取る。 */
class GetCallHistoriesUseCase @Inject constructor(
    private val repository: ContactRepository,
) {
    suspend operator fun invoke(): List<CallHistory> = repository.getCallHistories()
}
