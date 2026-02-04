package com.example.androidsampleapp.network

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer

/**
 * サーバの代わりに JSON を返す。mock flavor（`AppConfig.useFakeApi`）でだけ OkHttp に挟む。
 *
 * 通信の手前で応答を作って返すだけなので、Retrofit の呼び出し、JSON の解釈、
 * 2xx 以外で例外になるところまでは本物と同じ経路を通る。本物のサーバを使うときは、
 * この Interceptor を挟まなければそのまま本物を叩く。
 *
 * サーバが持つはずの状態（エアコンの現在値、着信、通話履歴と未確認の不在着信の件数、
 * 通知を消去したかどうか）もここで持つ。
 *
 * 着信は最初の状態取得から [CALL_DELAY_MS] 後に鳴り、[RING_DURATION_MS] のうちに応答か拒否が
 * 無ければ不在着信になる（件数を 1 つ増やし、履歴に足す）。どちらで終わっても、
 * [CALL_INTERVAL_MS] 後にまた鳴る。時間は要求が来たときに [now] から進める。
 * 知らないパスには 404、本文に端末の ID や必要な項目が無ければ 400 を返す。
 * 付け忘れに mock の段階で気づけるようにするため。
 */
class FakeApiInterceptor(
    /** 通信の遅れを画面で確認できるよう、少し待たせる。テストでは 0 にする。 */
    private val delayMs: Long = DEFAULT_DELAY_MS,
    private val now: () -> Long = System::currentTimeMillis,
) : Interceptor {

    private var airconIsOn = true
    private var airconMode = "COOL"
    private var targetTemperature = 26.0
    private val roomTemperature = 28.4

    /** 次に着信を鳴らす時刻。最初に状態を取られるまでは null（鳴らさない）。 */
    private var nextRingAt: Long? = null

    /** 鳴り始めた時刻。鳴っていなければ null。 */
    private var ringingSince: Long? = null

    /** 通話履歴。新しいものが先頭。 */
    private val histories = INITIAL_HISTORIES.toMutableList()
    private var nextHistoryId = INITIAL_HISTORIES.size + 1

    /** 未確認の不在着信の件数。既読で 0 になり、不在着信が起きるたびに増える。 */
    private var missedCallCount = INITIAL_HISTORIES.count { it.missed }

    /** 消去 API が呼ばれたか。以後の取得は空の配列を返す。 */
    private var noticesDeleted = false

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (delayMs > 0) Thread.sleep(delayMs)

        // OkHttp は複数のスレッドから呼ぶので、サーバの状態の読み書きをまとめて守る。
        val (code, body) = synchronized(this) { respond(request) }

        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(
                when (code) {
                    in 200..299 -> "OK"
                    400 -> "Bad Request"
                    else -> "Not Found"
                },
            )
            .body(body.toResponseBody(JSON))
            .build()
    }

    private fun respond(request: Request): Pair<Int, String> {
        val path = request.url.encodedPath
        val endpoint = ENDPOINTS.firstOrNull { path.endsWith("/$it") }
        if (request.method != "POST" || endpoint == null) return 404 to """{"error":"not found"}"""

        val body = bodyOf(request)
        if (body?.string("deviceId").isNullOrBlank()) return 400 to """{"error":"deviceId is required"}"""

        // どの API が呼ばれても、その時刻までに着信がどうなったかを先に進めておく。
        advanceCall()

        return when (endpoint) {
            "device/status" -> 200 to statusJson()

            "aircon/power" -> body!!.boolean("isOn")
                ?.let { airconIsOn = it; 200 to airconJson() }
                ?: BAD_REQUEST

            "aircon/mode" -> body!!.string("mode")
                ?.let { airconMode = it; 200 to airconJson() }
                ?: BAD_REQUEST

            "aircon/temperature" -> body!!.double("targetTemperature")
                ?.let { targetTemperature = it; 200 to airconJson() }
                ?: BAD_REQUEST

            "calls/answer", "calls/reject" -> {
                endCall(missed = false)
                204 to ""
            }

            "contacts/list" -> 200 to CONTACTS_JSON

            "calls/history" -> 200 to histories.joinToString(",", "[", "]") { it.toJson() }

            "missed-calls/count" -> 200 to """{"count":$missedCallCount}"""

            "missed-calls/read" -> {
                missedCallCount = 0
                204 to ""
            }

            "notices/list" -> 200 to if (noticesDeleted) "[]" else noticesJson(now())

            "notices/delete" -> {
                noticesDeleted = true
                204 to ""
            }

            else -> 404 to """{"error":"not found"}"""
        }
    }

    /** 本文の JSON を読む。本文が無い、または JSON のオブジェクトでないときは null。 */
    private fun bodyOf(request: Request): JsonObject? {
        val requestBody = request.body ?: return null
        val text = Buffer().also { requestBody.writeTo(it) }.readUtf8()
        return runCatching { Json.parseToJsonElement(text).jsonObject }.getOrNull()
    }

    private fun JsonObject.string(key: String): String? =
        runCatching { this[key]?.jsonPrimitive?.contentOrNull }.getOrNull()

    private fun JsonObject.boolean(key: String): Boolean? =
        runCatching { this[key]?.jsonPrimitive?.booleanOrNull }.getOrNull()

    private fun JsonObject.double(key: String): Double? =
        runCatching { this[key]?.jsonPrimitive?.doubleOrNull }.getOrNull()

    private fun statusJson(): String {
        if (nextRingAt == null) {
            // 最初の状態取得。ここから数えて鳴らす。
            nextRingAt = now() + CALL_DELAY_MS
        }
        val call = if (ringingSince != null) """{"roomId":"101","displayName":"$CALLER"}""" else "null"
        return """{"aircon":${airconJson()},"incomingCall":$call}"""
    }

    /** 鳴らす時刻が来ていれば鳴らし、鳴ったまま [RING_DURATION_MS] が過ぎていれば不在着信にする。 */
    private fun advanceCall() {
        val ringAt = nextRingAt ?: return
        val t = now()
        if (ringingSince == null && t >= ringAt) ringingSince = ringAt
        val since = ringingSince ?: return
        if (t >= since + RING_DURATION_MS) endCall(missed = true)
    }

    /** 鳴っている着信を終え、履歴に足して次の着信を予約する。鳴っていなければ何もしない。 */
    private fun endCall(missed: Boolean) {
        val since = ringingSince ?: return
        histories.add(0, FakeHistory("${nextHistoryId++}", CALLER, formatTime(since), missed))
        if (missed) missedCallCount++
        ringingSince = null
        // 取らずに長く放っておかれても、たまった分をまとめて不在にはしない。今から数え直す。
        nextRingAt = now() + CALL_INTERVAL_MS
    }

    private fun formatTime(epochMillis: Long): String =
        SimpleDateFormat("M月d日 HH:mm", Locale.JAPAN).format(Date(epochMillis))

    /** サーバが持つ通話履歴 1 件分。 */
    private data class FakeHistory(
        val id: String,
        val name: String,
        val occurredAt: String,
        val missed: Boolean,
    ) {
        fun toJson(): String =
            """{"id":"$id","name":"$name","occurredAt":"$occurredAt","missed":$missed}"""
    }

    private fun airconJson(): String =
        """{"isOn":$airconIsOn,"mode":"$airconMode","targetTemperature":$targetTemperature,""" +
            """"roomTemperature":$roomTemperature}"""

    /** 時刻は呼ばれた時点からさかのぼって置く。 */
    private fun noticesJson(now: Long): String = """
        [
          {
            "id": "1",
            "category": "CALL",
            "title": "不在着信",
            "message": "玄関からの呼び出しに応答がありませんでした",
            "occurredAt": ${now - 1 * HOUR_MS},
            "destination": "CONTACT_MISSED"
          },
          {
            "id": "2",
            "category": "ALERT",
            "title": "フィルター",
            "message": "フィルターの清掃時期です",
            "occurredAt": ${now - 3 * HOUR_MS},
            "destination": "AIRCON"
          },
          {
            "id": "3",
            "category": "AIRCON",
            "title": "リビング",
            "message": "設定温度を 26.0 度に変更しました",
            "occurredAt": ${now - 12 * HOUR_MS},
            "destination": "AIRCON"
          },
          {
            "id": "4",
            "category": "ALERT",
            "title": "故障情報：0402",
            "message": "室外機の通信が途絶えています\n点検を依頼してください",
            "occurredAt": ${now - 20 * HOUR_MS},
            "destination": "AIRCON"
          },
          {
            "id": "5",
            "category": "INFO",
            "title": null,
            "message": "システムを起動しました",
            "occurredAt": ${now - 30 * HOUR_MS},
            "destination": "TOP"
          }
        ]
    """.trimIndent()

    private companion object {
        const val DEFAULT_DELAY_MS = 500L
        const val HOUR_MS = 60 * 60 * 1000L

        /** 最初の状態取得から着信を鳴らすまで。起動直後の画面を確認する時間を取る。 */
        const val CALL_DELAY_MS = 8_000L

        /** 鳴らし続ける長さ。これを過ぎても応答か拒否が無ければ不在着信にする。 */
        const val RING_DURATION_MS = 20_000L

        /**
         * 着信が終わってから次を鳴らすまで。着信のたびにトップ画面へ移ってスリープも解けるので、
         * 短くしすぎると他の画面を確かめにくくなる。
         */
        const val CALL_INTERVAL_MS = 180_000L

        const val CALLER = "玄関"

        const val CONTACTS_JSON = """[
            {"id":"1","name":"管理室","phoneNumber":"0001"},
            {"id":"2","name":"玄関","phoneNumber":"0101"},
            {"id":"3","name":"駐車場","phoneNumber":"0102"},
            {"id":"4","name":"宅配ボックス","phoneNumber":"0103"}
        ]"""

        val INITIAL_HISTORIES = listOf(
            FakeHistory("1", "玄関", "9月11日 14:32", missed = true),
            FakeHistory("2", "管理室", "9月11日 10:05", missed = false),
            FakeHistory("3", "玄関", "9月10日 19:48", missed = false),
            FakeHistory("4", "駐車場", "9月10日 08:12", missed = true),
        )

        val ENDPOINTS = listOf(
            "device/status",
            "aircon/power",
            "aircon/mode",
            "aircon/temperature",
            "calls/answer",
            "calls/reject",
            "contacts/list",
            "calls/history",
            "missed-calls/count",
            "missed-calls/read",
            "notices/list",
            "notices/delete",
        )
        val BAD_REQUEST = 400 to """{"error":"bad request"}"""
        val JSON = "application/json; charset=utf-8".toMediaType()
    }
}
