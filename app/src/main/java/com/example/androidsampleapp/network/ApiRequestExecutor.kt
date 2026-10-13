package com.example.androidsampleapp.network

import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException

class ApiRequestExecutor @Inject constructor(private val json: Json) {
    /** Retrofit の「本文を直接返す suspend メソッド」を渡す。Unit の API にも使える。 */
    suspend fun <T> execute(request: suspend () -> T): ApiResult<T> =
        runCatching { request() }.fold(
            onSuccess = { ApiResult.Success(it) },
            onFailure = { cause ->
                when (cause) {
                    // runCatching はキャンセルも捕捉するので、失敗の値にせず投げ直す。
                    is CancellationException -> throw cause
                    is HttpException -> httpFailure(cause)
                    is Exception -> ApiResult.Failure(cause)
                    else -> throw cause
                }
            },
        )

    private fun httpFailure(cause: HttpException): ApiResult.HttpFailure {
        // errorBody は一度しか読めないので保存してから JSON に変換する。
        val rawBody = try {
            cause.response()?.errorBody()?.use { it.string() }
        } catch (_: java.io.IOException) {
            null
        }
        val response = rawBody?.let {
            try {
                json.decodeFromString<ApiErrorResponse>(it)
            } catch (_: kotlinx.serialization.SerializationException) {
                null
            } catch (_: IllegalArgumentException) {
                null
            }
        }
        return ApiResult.HttpFailure(cause.code(), response, rawBody, cause)
    }
}
