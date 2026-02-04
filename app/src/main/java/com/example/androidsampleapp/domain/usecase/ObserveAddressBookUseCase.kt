package com.example.androidsampleapp.domain.usecase

import com.example.androidsampleapp.domain.model.AddressBook
import com.example.androidsampleapp.domain.repository.ContactRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/** 電話帳と履歴を見る。取り直されるたびに新しい値が流れる。まだ取れていなければ null。 */
class ObserveAddressBookUseCase @Inject constructor(
    private val repository: ContactRepository,
) {
    operator fun invoke(): StateFlow<AddressBook?> = repository.addressBook
}
