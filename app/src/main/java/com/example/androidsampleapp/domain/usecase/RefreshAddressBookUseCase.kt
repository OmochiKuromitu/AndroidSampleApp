package com.example.androidsampleapp.domain.usecase

import com.example.androidsampleapp.domain.repository.ContactRepository
import javax.inject.Inject

/**
 * 電話帳と履歴を取り直す。結果は [ObserveAddressBookUseCase] の側に流れる。
 * 失敗したら例外を投げる。
 */
class RefreshAddressBookUseCase @Inject constructor(
    private val repository: ContactRepository,
) {
    suspend operator fun invoke() = repository.refreshAddressBook()
}
