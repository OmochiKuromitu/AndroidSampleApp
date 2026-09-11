package com.example.androidsampleapp.data

import com.example.androidsampleapp.domain.model.CallHistory
import com.example.androidsampleapp.domain.model.Contact
import com.example.androidsampleapp.domain.repository.ContactRepository
import com.example.androidsampleapp.model.CallHistoryResponse
import com.example.androidsampleapp.model.ContactResponse
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay

/**
 * サーバ側が未実装のため、今は仮データを返す。
 * 差し替えるのはこのクラスの中だけで、UseCase から上は変わらない。
 */
@Singleton
class ContactRepositoryImpl @Inject constructor() : ContactRepository {

    override suspend fun getContacts(): List<Contact> {
        // TODO: GET {AppConfig.apiBaseUrl}/contacts に置き換える。
        delay(API_DELAY_MS)
        return FAKE_CONTACTS.map { it.toDomain() }
    }

    override suspend fun getCallHistories(): List<CallHistory> {
        // TODO: GET {AppConfig.apiBaseUrl}/call-histories に置き換える。
        delay(API_DELAY_MS)
        return FAKE_HISTORIES.map { it.toDomain() }
    }

    override suspend fun getMissedCallCount(): Int {
        // TODO: GET {AppConfig.apiBaseUrl}/missed-calls に置き換える。
        delay(API_DELAY_MS)
        return FAKE_HISTORIES.count { it.missed }
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
