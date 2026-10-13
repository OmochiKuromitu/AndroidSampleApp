package com.example.androidsampleapp.network

/** 成功と失敗で本文の型が違っても、呼び出し元は when で安全に分岐できる。 */
sealed interface ApiResult<out T> {
    data class Success<T>(val response: T) : ApiResult<T>

    /** 本文が空、または想定した JSON でない場合も HTTP ステータスは保持する。 */
    data class HttpFailure(
        val statusCode: Int,
        val response: ApiErrorResponse?,
        val rawBody: String?,
        val cause: retrofit2.HttpException,
    ) : ApiResult<Nothing>

    /** 通信や成功レスポンスの変換に失敗した場合。サーバの本文は存在しないこともある。 */
    data class Failure(val cause: Exception) : ApiResult<Nothing>
}
