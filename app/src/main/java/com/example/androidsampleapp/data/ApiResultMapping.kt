package com.example.androidsampleapp.data

import com.example.androidsampleapp.domain.model.ApiError
import com.example.androidsampleapp.domain.model.ApiResult
import com.example.androidsampleapp.model.ApiErrorResponse
import com.example.androidsampleapp.model.ResultCodeResponse
import java.io.IOException
import java.io.InterruptedIOException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException

// Retrofit の呼び出し結果を ApiResult にする。Repository で次のように使う。
//
//   override suspend fun getXxxList(): ApiResult<XxxListResponse> =
//       runCatching { api.getXxxList() }.fold(
//           onSuccess = { it.toApiResult() },
//           onFailure = { it.toApiResult() },
//       )
//
// 結果コードを持たないレスポンスなら onSuccess = { ApiResult.Success(it) } と書く。
//
// onSuccess / onFailure（fold ではない方）は処理を挟むだけで戻り値の型を変えられないので、fold を使う。

/**
 * HTTP 200 で返った本文を、resultCode で成功と失敗に分ける。
 * 成功ならレスポンスをそのまま [ApiResult.Success] に包む。
 */
fun <R : ResultCodeResponse> R.toApiResult(): ApiResult<R> =
    if (resultCode == ResultCodeResponse.SUCCESS) {
        ApiResult.Success(this)
    } else {
        ApiResult.BusinessError(resultCode)
    }

/**
 * Retrofit / OkHttp が投げた例外を、失敗の種類ごとの [ApiResult] にする。
 * 4xx / 5xx でサーバがエラー本文を返していれば、[ApiError] にして持たせる。
 *
 * キャンセルは結果にせず投げ直す。runCatching はキャンセルも捕まえるので、
 * ここで投げ直さないと、閉じた画面に失敗の表示が流れる。
 */
fun Throwable.toApiResult(): ApiResult<Nothing> = when (this) {
    is CancellationException -> throw this
    // SocketTimeoutException（接続・読み込み）も、OkHttp の callTimeout（呼び出し全体）もこれの仲間。
    // どちらも IOException の仲間でもあるので、IOException より先に判定する。
    is InterruptedIOException -> ApiResult.Timeout
    is HttpException -> when (code()) {
        // サーバ側の時間切れ（408 Request Timeout / 504 Gateway Timeout）も、画面では同じタイムアウトとして扱う。
        408, 504 -> ApiResult.Timeout
        in 400..499 -> ApiResult.ClientError(code(), apiError())
        in 500..599 -> ApiResult.ServerError(code(), apiError())
        else -> ApiResult.Unknown(this)
    }
    is IOException -> ApiResult.NetworkError
    else -> ApiResult.Unknown(this)
}

/**
 * エラーの本文を [ApiError] にする。本文が空、または想定した JSON でなければ null。
 *
 * Retrofit は HttpException を作る前にエラーの本文を読み込み済みなので、ここで通信は起きない。
 * 本文は一度しか読めないので、ここ以外で読まないこと。
 */
private fun HttpException.apiError(): ApiError? {
    val body = try {
        response()?.errorBody()?.use { it.string() }
    } catch (e: IOException) {
        null
    }
    if (body.isNullOrBlank()) return null
    return try {
        errorJson.decodeFromString<ApiErrorResponse>(body).let { ApiError(code = it.code, message = it.message) }
    } catch (e: IllegalArgumentException) {
        // SerializationException もこれの仲間。HTML のエラーページなどが返ってきたときはここに来る。
        null
    }
}

/** di/NetworkModule の Json と同じく、サーバが項目を足しても落ちないようにする。 */
private val errorJson = Json { ignoreUnknownKeys = true }
