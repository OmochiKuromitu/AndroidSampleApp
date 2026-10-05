package com.example.androidsampleapp.data

import com.example.androidsampleapp.config.AppConfig
import com.example.androidsampleapp.core.MissedCallManager
import com.example.androidsampleapp.di.NetworkModule
import com.example.androidsampleapp.domain.model.NoticeCategory
import com.example.androidsampleapp.domain.model.NoticeDestination
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class NoticeRepositoryImplTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: NoticeRepositoryImpl

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
        repository = NoticeRepositoryImpl(NetworkModule.provideNoticeApi(retrofit), config, MissedCallManager())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `取り直すまでは未取得で読み込み中`() {
        assertFalse(repository.noticeSnapshot.value.isLoaded)
        assertTrue(repository.noticeSnapshot.value.isLoading)
    }

    @Test
    fun `POST notices-list の JSON を読んでドメインの型にする`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """
                [
                  {"id":"1","category":"ALERT","title":"フィルター","message":"清掃時期です",
                   "occurredAt":1000,"destination":"AIRCON","pinned":true,"unknownField":"無視される"},
                  {"id":"2","category":"INFO","message":"起動しました","occurredAt":500,"destination":"TOP"}
                ]
                """.trimIndent(),
            ),
        )

        repository.refreshNotices()

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/notices/list", request.path)
        assertEquals(EXPECTED_BODY, request.body.readUtf8())

        val notices = repository.noticeSnapshot.value.notices
        assertEquals(listOf("1", "2"), notices.map { it.id })
        assertEquals(NoticeCategory.ALERT, notices[0].category)
        assertEquals(NoticeDestination.Aircon, notices[0].destination)
        assertEquals(1000L, notices[0].occurredAt)
        assertTrue(notices[0].isPinned)
        // title と pinned は項目ごと無くても読める。pinned が無ければ優先しない通知。
        assertNull(notices[1].title)
        assertFalse(notices[1].isPinned)
    }

    @Test
    fun `POST notices-delete のあとに取り直し、消去中に届いた通知も反映する`() = runTest {
        // 消去と再取得の順序、共有状態への反映は Repository が担当する。
        server.enqueue(MockResponse().setBody("""[{"id":"1","category":"INFO","message":"m","occurredAt":1,"destination":"TOP"}]"""))
        server.enqueue(MockResponse().setResponseCode(204))
        server.enqueue(MockResponse().setBody("""[{"id":"new","category":"INFO","message":"m","occurredAt":2,"destination":"TOP"}]"""))
        repository.refreshNotices()
        server.takeRequest()

        repository.deleteAllNotices()

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/notices/delete", request.path)
        assertEquals(EXPECTED_BODY, request.body.readUtf8())
        val listRequest = server.takeRequest()
        assertEquals("/api/notices/list", listRequest.path)
        assertEquals(EXPECTED_BODY, listRequest.body.readUtf8())
        assertEquals(listOf("new"), repository.noticeSnapshot.value.notices.map { it.id })
    }

    @Test
    fun `取得に失敗したら例外を投げ、前回の一覧を残す`() = runTest {
        server.enqueue(MockResponse().setBody("""[{"id":"1","category":"INFO","message":"m","occurredAt":1,"destination":"TOP"}]"""))
        server.enqueue(MockResponse().setResponseCode(500))
        repository.refreshNotices()

        try {
            repository.refreshNotices()
            fail("例外が投げられるはず")
        } catch (e: HttpException) {
            assertEquals(500, e.code())
        }

        assertEquals(listOf("1"), repository.noticeSnapshot.value.notices.map { it.id })
        assertTrue(repository.noticeSnapshot.value.loadFailed)
        assertFalse(repository.noticeSnapshot.value.isLoading)
    }

    @Test
    fun `空の配列なら空の一覧が入る`() = runTest {
        // 「まだ取っていない（null）」と「取ったら空だった」を分ける。
        server.enqueue(MockResponse().setBody("[]"))

        repository.refreshNotices()

        assertTrue(repository.noticeSnapshot.value.notices.isEmpty())
        assertTrue(repository.noticeSnapshot.value.isLoaded)
        assertFalse(repository.noticeSnapshot.value.isLoading)
    }

    private companion object {
        const val DEVICE_ID = "panel-test"

        /** 取得も消去も、本文で端末の ID を名乗る。 */
        const val EXPECTED_BODY = """{"deviceId":"$DEVICE_ID"}"""
    }
}
