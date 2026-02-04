package com.example.androidsampleapp.data

import com.example.androidsampleapp.config.AppConfig
import com.example.androidsampleapp.domain.model.AddressBook
import com.example.androidsampleapp.domain.model.CallHistory
import com.example.androidsampleapp.domain.model.Contact
import com.example.androidsampleapp.domain.repository.ContactRepository
import com.example.androidsampleapp.model.CallHistoryResponse
import com.example.androidsampleapp.model.ContactResponse
import com.example.androidsampleapp.model.DeviceRequest
import com.example.androidsampleapp.network.ContactApi
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 電話帳・通話履歴・不在着信は HTTP の API（[ContactApi]）から取る。
 *
 * 電話帳と履歴は、取った結果を [addressBook] に持つ。@Singleton なので、
 * 画面を閉じて開き直しても前回の値がすぐ出て、取り直しが終われば差し替わる。
 * 不在着信の件数は MissedCallManager が保持するので、ここは呼ばれたら都度取りに行くだけ。
 *
 * API はどの端末からの要求かを本文で名乗らせる。端末の ID は [AppConfig.deviceId] から詰める。
 * ID が要るのは通信の都合なので、ここで閉じる。[ContactRepository] の口は変えず、呼ぶ側は ID を知らない。
 *
 * mock flavor では OkHttp に挟んだ FakeApiInterceptor が JSON を返すので、
 * このクラスはサーバの有無を知らない。
 */
@Singleton
class ContactRepositoryImpl @Inject constructor(
    private val api: ContactApi,
    private val config: AppConfig,
) : ContactRepository {

    private val _addressBook = MutableStateFlow<AddressBook?>(null)
    override val addressBook: StateFlow<AddressBook?> = _addressBook.asStateFlow()

    override suspend fun refreshAddressBook() {
        // 両方そろってから入れる。片方だけ新しい値が流れて、画面に食い違った組が出るのを防ぐ。
        // 失敗したら例外がそのまま上がり、[addressBook] は前回の値のまま残る。
        val contacts = api.getContacts(request()).map { it.toDomain() }
        val histories = api.getCallHistories(request()).map { it.toDomain() }
        _addressBook.value = AddressBook(contacts = contacts, histories = histories)
    }

    override suspend fun getMissedCallCount(): Int = api.getMissedCallCount(request()).count

    override suspend fun markMissedCallsAsRead() {
        api.markMissedCallsAsRead(request())
    }

    private fun request(): DeviceRequest = DeviceRequest(deviceId = config.deviceId)

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
}
