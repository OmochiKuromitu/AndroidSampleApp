package com.example.androidsampleapp.domain.usecase

import com.example.androidsampleapp.domain.model.Contact
import com.example.androidsampleapp.domain.repository.ContactRepository
import javax.inject.Inject

/** 電話帳を取得 API から取る。 */
class GetContactsUseCase @Inject constructor(
    private val repository: ContactRepository,
) {
    suspend operator fun invoke(): List<Contact> = repository.getContacts()
}
