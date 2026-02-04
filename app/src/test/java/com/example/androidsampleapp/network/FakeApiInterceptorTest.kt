package com.example.androidsampleapp.network

import com.example.androidsampleapp.di.NetworkModule
import com.example.androidsampleapp.model.AirconModeRequest
import com.example.androidsampleapp.model.AirconPowerRequest
import com.example.androidsampleapp.model.CallRequest
import com.example.androidsampleapp.model.DeviceRequest
import com.example.androidsampleapp.model.NoticeResponse
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET

/**
 * mock flavor の擬似 API が、本物の Retrofit / JSON の解釈を通して読めることを確かめる。
 * 返す JSON の形を変えたときに、アプリを起動する前にここで気づけるようにする。
 */
class FakeApiInterceptorTest {

    /** 擬似 API が知らないパスを叩くためだけの口。 */
    private interface UnknownApi {
        @GET("unknown")
        suspend fun get(): List<NoticeResponse>
    }

    /** 擬似 API が読む時刻。着信が鳴るまでの時間を進めるために書き換える。 */
    private var now = NOW

    private val retrofit: Retrofit = Retrofit.Builder()
        // 通信の手前で応答が返るので、この宛先に実際には繋がない。
        .baseUrl("http://127.0.0.1:1/api/")
        .client(OkHttpClient.Builder().addInterceptor(FakeApiInterceptor(delayMs = 0, now = { now })).build())
        .addConverterFactory(NetworkModule.provideJson().asConverterFactory("application/json".toMediaType()))
        .build()

    private val api = retrofit.create(NoticeApi::class.java)
    private val deviceApi = retrofit.create(DeviceApi::class.java)
    private val contactApi = retrofit.create(ContactApi::class.java)

    /** 最初の状態取得をして、着信が鳴るところまで時刻を進める。 */
    private suspend fun ringCall() {
        deviceApi.getStatus(REQUEST)
        now += CALL_DELAY_MS
        assertNotNull(deviceApi.getStatus(REQUEST).incomingCall)
    }

    @Test
    fun `状態は最初の取得から少し経つと着信ありになり、応答すると消える`() = runTest {
        val first = deviceApi.getStatus(REQUEST)
        assertNull(first.incomingCall)
        assertTrue(first.aircon.isOn)

        now += CALL_DELAY_MS
        assertNotNull(deviceApi.getStatus(REQUEST).incomingCall)

        deviceApi.answerCall(CallRequest(deviceId = DEVICE_ID, roomId = "101"))
        assertNull(deviceApi.getStatus(REQUEST).incomingCall)
    }

    @Test
    fun `着信を取らずに鳴り終わると、不在着信の件数が増えて履歴にも残る`() = runTest {
        assertEquals(2, contactApi.getMissedCallCount(REQUEST).count)
        ringCall()

        now += RING_DURATION_MS
        assertNull(deviceApi.getStatus(REQUEST).incomingCall)

        assertEquals(3, contactApi.getMissedCallCount(REQUEST).count)
        val latest = contactApi.getCallHistories(REQUEST).first()
        assertTrue(latest.missed)
        assertEquals("玄関", latest.name)
        assertEquals(5, contactApi.getCallHistories(REQUEST).size)
    }

    @Test
    fun `状態を取りに来なくても、件数を取った時点で不在着信になっている`() = runTest {
        // バッジはタブ移動で件数だけを取るので、状態取得を挟まなくても進んでいないといけない。
        ringCall()

        now += RING_DURATION_MS

        assertEquals(3, contactApi.getMissedCallCount(REQUEST).count)
    }

    @Test
    fun `応答した着信は不在にならず、履歴には応答として残る`() = runTest {
        ringCall()
        deviceApi.answerCall(CallRequest(deviceId = DEVICE_ID, roomId = "101"))

        now += RING_DURATION_MS

        assertEquals(2, contactApi.getMissedCallCount(REQUEST).count)
        assertFalse(contactApi.getCallHistories(REQUEST).first().missed)
    }

    @Test
    fun `着信が終わってからしばらくすると、また鳴る`() = runTest {
        ringCall()
        deviceApi.rejectCall(CallRequest(deviceId = DEVICE_ID, roomId = "101"))

        now += CALL_INTERVAL_MS - 1
        assertNull(deviceApi.getStatus(REQUEST).incomingCall)
        now += 1
        assertNotNull(deviceApi.getStatus(REQUEST).incomingCall)
    }

    @Test
    fun `既読にすると件数が 0 になり、履歴の不在の印は残る`() = runTest {
        contactApi.markMissedCallsAsRead(REQUEST)

        assertEquals(0, contactApi.getMissedCallCount(REQUEST).count)
        assertEquals(2, contactApi.getCallHistories(REQUEST).count { it.missed })
    }

    @Test
    fun `電話帳の JSON を返し、そのまま読める`() = runTest {
        val contacts = contactApi.getContacts(REQUEST)

        assertEquals(4, contacts.size)
        assertEquals("管理室", contacts[0].name)
    }

    @Test
    fun `エアコンの操作は変えたあとの値を返し、状態にも残る`() = runTest {
        val off = deviceApi.setAirconPower(AirconPowerRequest(deviceId = DEVICE_ID, isOn = false))
        assertFalse(off.isOn)

        val dry = deviceApi.setAirconMode(AirconModeRequest(deviceId = DEVICE_ID, mode = "DRY"))
        assertEquals("DRY", dry.mode)

        val status = deviceApi.getStatus(REQUEST).aircon
        assertFalse(status.isOn)
        assertEquals("DRY", status.mode)
    }

    @Test
    fun `通知の JSON を返し、そのまま読める`() = runTest {
        val notices = api.getNotices(REQUEST)

        assertEquals(listOf("1", "2", "3", "4", "5"), notices.map { it.id })
        assertEquals(NOW - HOUR_MS, notices[0].occurredAt)
        assertEquals("室外機の通信が途絶えています\n点検を依頼してください", notices[3].message)
        assertNull(notices[4].title)
    }

    @Test
    fun `消去したあとは空の配列を返す`() = runTest {
        api.deleteAllNotices(REQUEST)

        assertTrue(api.getNotices(REQUEST).isEmpty())
    }

    @Test
    fun `端末の ID が空なら 400 になり、消去もされない`() = runTest {
        try {
            api.deleteAllNotices(DeviceRequest(deviceId = ""))
            fail("例外が投げられるはず")
        } catch (e: HttpException) {
            assertEquals(400, e.code())
        }

        assertEquals(5, api.getNotices(REQUEST).size)
    }

    @Test
    fun `知らないパスは 404 になる`() = runTest {
        try {
            retrofit.create(UnknownApi::class.java).get()
            fail("例外が投げられるはず")
        } catch (e: HttpException) {
            assertEquals(404, e.code())
        }
    }

    private companion object {
        const val DEVICE_ID = "device-test"
        val REQUEST = DeviceRequest(deviceId = DEVICE_ID)
        const val NOW = 1_789_106_400_000L

        /** FakeApiInterceptor が最初の状態取得から着信を鳴らすまでの時間。 */
        const val CALL_DELAY_MS = 8_000L

        /** FakeApiInterceptor が鳴らし続ける長さ。過ぎると不在着信になる。 */
        const val RING_DURATION_MS = 20_000L

        /** FakeApiInterceptor が着信を終えてから次を鳴らすまでの時間。 */
        const val CALL_INTERVAL_MS = 180_000L
        const val HOUR_MS = 60 * 60 * 1000L
    }
}
