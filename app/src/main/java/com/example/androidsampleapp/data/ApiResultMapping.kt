package com.example.androidsampleapp.data

import com.example.androidsampleapp.domain.model.ApiResult
import com.example.androidsampleapp.model.ResultCodeResponse
import java.io.IOException
import java.io.InterruptedIOException
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

// Retrofit の呼び出し結果を ApiResult にする。Repository で次のように使う。
//
//   override suspend fun getXxxList(): ApiResult<List<Xxx>> =
//       runCatching { api.getXxxList() }.fold(
//           onSuccess = { response -> response.toApiResult { it.items.map { item -> item.toDomain() } } },
//           onFailure = { it.toApiResult() },
//       )
//
// onSuccess / onFailure（fold ではない方）は処理を挟むだけで戻り値の型を変えられないので、fold を使う。

/**
 * HTTP 200 で返った本文を、resultCode で成功と失敗に分ける。
 * 成功なら [transform] でドメインの型に変えて [ApiResult.Success] に包む。
 */
inline fun <R : ResultCodeResponse, T> R.toApiResult(transform: (R) -> T): ApiResult<T> =
    if (resultCode == ResultCodeResponse.SUCCESS) {
        ApiResult.Success(transform(this))
    } else {
        ApiResult.BusinessError(resultCode)
    }

/**
 * Retrofit / OkHttp が投げた例外を、失敗の種類ごとの [ApiResult] にする。
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
        in 400..499 -> ApiResult.ClientError(code())
        in 500..599 -> ApiResult.ServerError(code())
        else -> ApiResult.Unknown(this)
    }
    is IOException -> ApiResult.NetworkError
    else -> ApiResult.Unknown(this)
}
