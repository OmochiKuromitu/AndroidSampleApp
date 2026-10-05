package com.example.androidsampleapp.data

import com.example.androidsampleapp.config.AppConfig
import com.example.androidsampleapp.core.MissedCallManager
import com.example.androidsampleapp.model.CallHistoryResponse
import com.example.androidsampleapp.model.ContactResponse
import com.example.androidsampleapp.model.DeviceRequest
import com.example.androidsampleapp.model.MissedCallCountResponse
import com.example.androidsampleapp.model.NoticeResponse
import com.example.androidsampleapp.network.ContactApi
import com.example.androidsampleapp.network.NoticeApi
import java.io.IOException
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

/** 通信だけを偽にして、本物の Repository と共有状態で競合・キャンセルを検証する。 */
internal class NotificationTestFixture {
    val manager = MissedCallManager()
    val contactApi = FakeContactApi()
    val noticeApi = FakeNoticeApi()
    private val config = AppConfig("http://localhost/api", false, "test-panel", 30.seconds)
    val contactRepository = ContactRepositoryImpl(contactApi, config, manager)
    val noticeRepository = NoticeRepositoryImpl(noticeApi, config, manager)
}

internal class FakeContactApi : ContactApi {
    val calls = mutableListOf<String>()
    var count = 5
    var countAfterRead = 0
    var failOnGet = false
    var failOnRead = false
    var countResponse: suspend () -> Int = {
        // 要求時点の値を返す。並行取得を許すと、既読前の値が後から届き得る。
        val result = count
        delay(100)
        if (failOnGet) throw IOException("count failed")
        result
    }
    var readResponse: suspend () -> Unit = {
        delay(100)
        if (failOnRead) throw IOException("read failed")
        count = countAfterRead
    }

    override suspend fun getContacts(body: DeviceRequest): List<ContactResponse> = emptyList()
    override suspend fun getCallHistories(body: DeviceRequest): List<CallHistoryResponse> = emptyList()

    override suspend fun getMissedCallCount(body: DeviceRequest): MissedCallCountResponse {
        calls += "count"
        return MissedCallCountResponse(countResponse())
    }

    override suspend fun markMissedCallsAsRead(body: DeviceRequest) {
        calls += "read"
        readResponse()
    }
}

internal class FakeNoticeApi : NoticeApi {
    val calls = mutableListOf<String>()
    var notices = listOf(noticeResponse("old", 100))
    var noticesAfterDelete = emptyList<NoticeResponse>()
    var failOnGet = false
    var failOnDelete = false
    var listResponse: suspend () -> List<NoticeResponse> = {
        val result = notices
        delay(100)
        if (failOnGet) throw IOException("list failed")
        result
    }
    var deleteResponse: suspend () -> Unit = {
        delay(100)
        if (failOnDelete) throw IOException("delete failed")
        notices = noticesAfterDelete
    }

    override suspend fun getNotices(body: DeviceRequest): List<NoticeResponse> {
        calls += "list"
        return listResponse()
    }

    override suspend fun deleteAllNotices(body: DeviceRequest) {
        calls += "delete"
        deleteResponse()
    }
}

internal fun noticeResponse(id: String, occurredAt: Long) = NoticeResponse(
    id = id,
    category = "INFO",
    message = "本文",
    occurredAt = occurredAt,
    destination = "TOP",
)
