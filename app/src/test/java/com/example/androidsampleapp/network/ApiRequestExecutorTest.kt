package com.example.androidsampleapp.network

import com.example.androidsampleapp.di.NetworkModule
import com.example.androidsampleapp.model.DeviceRequest
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class ApiRequestExecutorTest {
    private val json = NetworkModule.provideJson()
    private val executor = ApiRequestExecutor(json)

    @Test
    fun `Retrofit の成功と HTTP エラーの異なる本文を返す`() = runTest {
        val server = MockWebServer()
        server.start()
        try {
            val api = Retrofit.Builder()
                .baseUrl(server.url("/"))
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build().create(ContactApi::class.java)
            val client = ContactApiClient(api, executor)
            val request = DeviceRequest("test-device")

            server.enqueue(MockResponse().setBody("""[{"id":"1","name":"管理室","phoneNumber":"0001"}]"""))
            val success = client.getContacts(request) as ApiResult.Success
            assertEquals("管理室", success.response.single().name)

            server.enqueue(MockResponse().setResponseCode(400)
                .setBody("""{"code":"INVALID_DEVICE","message":"端末が不正です"}"""))
            val failure = client.getContacts(request) as ApiResult.HttpFailure
            assertEquals(400, failure.statusCode)
            assertEquals(ApiErrorResponse("INVALID_DEVICE", "端末が不正です"), failure.response)

            server.enqueue(MockResponse().setResponseCode(503).setBody("unavailable"))
            val invalidBody = client.getContacts(request) as ApiResult.HttpFailure
            assertEquals(503, invalidBody.statusCode)
            assertNull(invalidBody.response)
            assertEquals("unavailable", invalidBody.rawBody)

            server.enqueue(MockResponse().setResponseCode(500))
            val emptyBody = client.getContacts(request) as ApiResult.HttpFailure
            assertEquals(500, emptyBody.statusCode)
            assertNull(emptyBody.response)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `通信例外を失敗として返す`() = runTest {
        val cause = IOException("offline")
        val result = executor.execute<Unit> { throw cause } as ApiResult.Failure
        assertSame(cause, result.cause)
    }

    @Test
    fun `本文のない成功も返せる`() = runTest {
        assertEquals(ApiResult.Success(Unit), executor.execute { Unit })
    }

    @Test
    fun `キャンセルは失敗として返さず伝播する`() = runTest {
        val cancellation = CancellationException("cancelled")
        try {
            executor.execute<Unit> { throw cancellation }
            fail("CancellationException が必要")
        } catch (actual: CancellationException) {
            assertSame(cancellation, actual)
        }
    }
}
