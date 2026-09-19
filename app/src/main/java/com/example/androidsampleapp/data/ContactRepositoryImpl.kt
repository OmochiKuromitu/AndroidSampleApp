package com.example.androidsampleapp.data

import com.example.androidsampleapp.domain.model.AddressBook
import com.example.androidsampleapp.domain.model.CallHistory
import com.example.androidsampleapp.domain.model.Contact
import com.example.androidsampleapp.domain.repository.ContactRepository
import com.example.androidsampleapp.model.CallHistoryResponse
import com.example.androidsampleapp.model.ContactResponse
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 電話帳と履歴は、取った結果を [addressBook] に持つ。@Singleton なので、
 * 画面を閉じて開き直しても前回の値がすぐ出て、取り直しが終われば差し替わる。
 *
 * サーバ側が未実装のため、今は仮データを返す。
 * 差し替えるのはこのクラスの中だけで、UseCase から上は変わらない。
 */
@Singleton
class ContactRepositoryImpl @Inject constructor() : ContactRepository {

    /** TODO: API ができたら削除する。本来はサーバが持つ、未確認の不在着信の件数。 */
    private var fakeServerMissedCallCount: Int = FAKE_HISTORIES.count { it.missed }

    private val _addressBook = MutableStateFlow<AddressBook?>(null)
    override val addressBook: StateFlow<AddressBook?> = _addressBook.asStateFlow()

    override suspend fun refreshAddressBook() {
        // 両方そろってから入れる。片方だけ新しい値が流れて、画面に食い違った組が出るのを防ぐ。
        val contacts = fetchContacts()
        val histories = fetchCallHistories()
        _addressBook.value = AddressBook(contacts = contacts, histories = histories)
    }

    private suspend fun fetchContacts(): List<Contact> {
        // TODO: GET {AppConfig.apiBaseUrl}/contacts に置き換える。
        delay(API_DELAY_MS)
        return FAKE_CONTACTS.map { it.toDomain() }
    }

    private suspend fun fetchCallHistories(): List<CallHistory> {
        // TODO: GET {AppConfig.apiBaseUrl}/call-histories に置き換える。
        delay(API_DELAY_MS)
        return FAKE_HISTORIES.map { it.toDomain() }
    }

    override suspend fun getMissedCallCount(): Int {
        // TODO: GET {AppConfig.apiBaseUrl}/missed-calls に置き換える。
        delay(API_DELAY_MS)
        return fakeServerMissedCallCount
    }

    override suspend fun markMissedCallsAsRead() {
        // TODO: POST {AppConfig.apiBaseUrl}/missed-calls/read に置き換える。
        delay(API_DELAY_MS)
        fakeServerMissedCallCount = 0
    }

    private fun ContactResponse.toDomain(): Contact = Contact(
        id = id,
        name = name,
        phoneNumber = phoneNumber,
    )

    private fun CallHistoryResponse.toDomain(): CallHistory = CallHistory(
        id = id,
        name = name,
        occurredAt = occurredAt,
        isMissed = missed,
    )

    private companion object {
        const val API_DELAY_MS = 400L

        val FAKE_CONTACTS = listOf(
            ContactResponse("1", "管理室", "0001"),
            ContactResponse("2", "玄関", "0101"),
            ContactResponse("3", "駐車場", "0102"),
            ContactResponse("4", "宅配ボックス", "0103"),
        )

        val FAKE_HISTORIES = listOf(
            CallHistoryResponse("1", "玄関", "9月11日 14:32", missed = true),
            CallHistoryResponse("2", "管理室", "9月11日 10:05", missed = false),
            CallHistoryResponse("3", "玄関", "9月10日 19:48", missed = false),
            CallHistoryResponse("4", "駐車場", "9月10日 08:12", missed = true),
        )
    }
}
