package com.example.androidsampleapp.data

import com.example.androidsampleapp.di.NetworkModule
import com.example.androidsampleapp.domain.model.ApiError
import com.example.androidsampleapp.domain.model.ApiResult
import com.example.androidsampleapp.domain.model.apiError
import com.example.androidsampleapp.model.ResultCodeResponse
import java.io.InterruptedIOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.POST

/**
 * 手元に立てた HTTP サーバに本物の Retrofit / OkHttp で投げ、
 * Repository と同じ書き方（runCatching + fold）で [ApiResult] になるかを確かめる。
 */
class ApiResultMappingTest {

    private lateinit var server: MockWebServer
    private lateinit var api: ApiResultSampleApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val client = OkHttpClient.Builder()
            // タイムアウトの確認を待ちすぎないよう短くする。
            .readTimeout(200, TimeUnit.MILLISECONDS)
            // 切断のときに自動で再接続されると、次の応答を待ってタイムアウトになってしまう。
            .retryOnConnectionFailure(false)
            .build()
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .addConverterFactory(NetworkModule.provideJson().asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ApiResultSampleApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    /** Repository に書くのと同じ形。 */
    private suspend fun call(): ApiResult<List<String>> =
        runCatching { api.get() }.fold(
            onSuccess = { response -> response.toApiResult { it.items } },
            onFailure = { it.toApiResult() },
        )

    @Test
    fun `200 で resultCode が 0 なら成功`() = runTest {
        server.enqueue(MockResponse().setBody("""{"resultCode":0,"items":["a","b"]}"""))

        assertEquals(ApiResult.Success(listOf("a", "b")), call())
    }

    @Test
    fun `200 でも resultCode が 0 以外なら BusinessError`() = runTest {
        // 失敗のときは items が無くても読める。
        server.enqueue(MockResponse().setBody("""{"resultCode":3}"""))

        assertEquals(ApiResult.BusinessError(3), call())
    }

    @Test
    fun `4xx は ClientError`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))

        assertEquals(ApiResult.ClientError(404), call())
    }

    @Test
    fun `5xx は ServerError`() = runTest {
        server.enqueue(MockResponse().setResponseCode(503))

        assertEquals(ApiResult.ServerError(503), call())
    }

    @Test
    fun `4xx でエラー本文があれば code と message を持たせる`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(400)
                .setBody("""{"code":"INVALID_DEVICE","message":"端末が不正です","extra":1}"""),
        )

        val result = call()

        assertEquals(ApiResult.ClientError(400, ApiError("INVALID_DEVICE", "端末が不正です")), result)
        assertEquals("端末が不正です", result.apiError?.message)
    }

    @Test
    fun `5xx でエラー本文があれば code と message を持たせる`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(500)
                .setBody("""{"code":"MAINTENANCE","message":"メンテナンス中です"}"""),
        )

        assertEquals(ApiResult.ServerError(500, ApiError("MAINTENANCE", "メンテナンス中です")), call())
    }

    @Test
    fun `エラー本文が想定した JSON でなければ error は null`() = runTest {
        // プロキシや Web サーバが返す HTML のエラーページなど。
        server.enqueue(MockResponse().setResponseCode(502).setBody("<html>Bad Gateway</html>"))

        assertEquals(ApiResult.ServerError(502, null), call())
    }

    @Test
    fun `応答が来なければ Timeout`() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))

        assertEquals(ApiResult.Timeout, call())
    }

    @Test
    fun `接続が切れたら NetworkError`() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        assertEquals(ApiResult.NetworkError, call())
    }

    @Test
    fun `JSON が読めなければ Unknown`() = runTest {
        server.enqueue(MockResponse().setBody("not json"))

        assertTrue(call() is ApiResult.Unknown)
    }

    @Test
    fun `408 と 504 は HTTP エラーではなく Timeout`() = runTest {
        server.enqueue(MockResponse().setResponseCode(408))
        assertEquals(ApiResult.Timeout, call())

        server.enqueue(MockResponse().setResponseCode(504).setBody("""{"code":"UPSTREAM","message":"応答がありません"}"""))
        assertEquals(ApiResult.Timeout, call())
    }

    @Test
    fun `OkHttp の callTimeout も Timeout`() {
        // callTimeout は SocketTimeoutException ではなく InterruptedIOException("timeout") で来る。
        assertEquals(ApiResult.Timeout, InterruptedIOException("timeout").toApiResult())
    }

    @Test
    fun `キャンセルは結果にせず投げ直す`() {
        val cancellation = CancellationException("cancelled")
        try {
            cancellation.toApiResult()
            fail("CancellationException が投げ直されるはず")
        } catch (actual: CancellationException) {
            assertSame(cancellation, actual)
        }
    }
}

// Retrofit とシリアライザがリフレクションで触るので、private にせずファイルの外に出す。

@Serializable
internal data class ApiResultSampleResponse(
    override val resultCode: Int,
    val items: List<String> = emptyList(),
) : ResultCodeResponse

internal interface ApiResultSampleApi {
    @POST("sample")
    suspend fun get(): ApiResultSampleResponse
}
