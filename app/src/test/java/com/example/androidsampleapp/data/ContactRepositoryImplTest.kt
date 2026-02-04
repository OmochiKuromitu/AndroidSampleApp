package com.example.androidsampleapp.data

import com.example.androidsampleapp.config.AppConfig
import com.example.androidsampleapp.di.NetworkModule
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException

/**
 * 手元に立てた HTTP サーバ（MockWebServer）に対して、本物の Retrofit / OkHttp で叩く。
 * 組み立ては NetworkModule をそのまま使い、アプリと同じ設定を通す。
 */
class ContactRepositoryImplTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: ContactRepositoryImpl

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val config = AppConfig(
            // 設定には末尾の `/` を付けない約束。
            apiBaseUrl = server.url("/api").toString().trimEnd('/'),
            useFakeApi = false,
            deviceId = DEVICE_ID,
            sleepTimeout = 30.seconds,
        )
        val retrofit = NetworkModule.provideRetrofit(
            config,
            NetworkModule.provideOkHttpClient(config),
            NetworkModule.provideJson(),
        )
        repository = ContactRepositoryImpl(NetworkModule.provideContactApi(retrofit), config)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `取り直すまでは電話帳と履歴が null`() {
        // 画面が「まだ取っていない」と「取ったら空だった」を区別できるようにする。
        assertNull(repository.addressBook.value)
    }

    @Test
    fun `電話帳と履歴を POST で取り、そろえて入れる`() = runTest {
        server.enqueue(MockResponse().setBody(CONTACTS))
        server.enqueue(MockResponse().setBody(HISTORIES))

        repository.refreshAddressBook()

        val contactsRequest = server.takeRequest()
        assertEquals("POST", contactsRequest.method)
        assertEquals("/api/contacts/list", contactsRequest.path)
        assertEquals(DEVICE_BODY, contactsRequest.body.readUtf8())
        val historiesRequest = server.takeRequest()
        assertEquals("/api/calls/history", historiesRequest.path)
        assertEquals(DEVICE_BODY, historiesRequest.body.readUtf8())

        val book = repository.addressBook.value!!
        assertEquals(listOf("管理室"), book.contacts.map { it.name })
        assertEquals("0001", book.contacts[0].phoneNumber)
        assertEquals(listOf("h1"), book.histories.map { it.id })
        assertTrue(book.histories[0].isMissed)
        assertEquals("9月11日 14:32", book.histories[0].occurredAt)
    }

    @Test
    fun `履歴の取得に失敗したら例外を投げ、前回の電話帳と履歴を残す`() = runTest {
        // 片方だけ新しい組を見せない。
        server.enqueue(MockResponse().setBody(CONTACTS))
        server.enqueue(MockResponse().setBody(HISTORIES))
        repository.refreshAddressBook()
        server.enqueue(MockResponse().setBody("[]"))
        server.enqueue(MockResponse().setResponseCode(500))

        try {
            repository.refreshAddressBook()
            fail("例外が投げられるはず")
        } catch (e: HttpException) {
            assertEquals(500, e.code())
        }

        assertEquals(listOf("管理室"), repository.addressBook.value!!.contacts.map { it.name })
    }

    @Test
    fun `不在着信の件数を POST で取る`() = runTest {
        server.enqueue(MockResponse().setBody("""{"count":3}"""))

        assertEquals(3, repository.getMissedCallCount())

        val request = server.takeRequest()
        assertEquals("/api/missed-calls/count", request.path)
        assertEquals(DEVICE_BODY, request.body.readUtf8())
    }

    @Test
    fun `既読を POST で送る`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))

        repository.markMissedCallsAsRead()

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/missed-calls/read", request.path)
        assertEquals(DEVICE_BODY, request.body.readUtf8())
    }

    private companion object {
        const val DEVICE_ID = "panel-test"
        const val DEVICE_BODY = """{"deviceId":"$DEVICE_ID"}"""
        const val CONTACTS = """[{"id":"1","name":"管理室","phoneNumber":"0001"}]"""
        const val HISTORIES =
            """[{"id":"h1","name":"玄関","occurredAt":"9月11日 14:32","missed":true}]"""
    }
}
