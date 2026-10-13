package com.example.androidsampleapp.domain.model

/**
 * API を 1 回呼んだ結果。Repository が返し、ViewModel は when で出し分ける。
 *
 * 例外を投げずに戻り値で失敗を返すので、呼ぶ側に try / catch は要らない。
 * キャンセルだけは結果にせず、例外のまま伝える（画面を離れたあとに失敗の表示を出さないため）。
 *
 * [Success] の中身は、Retrofit のレスポンスをそのまま入れてよい。変換が要るなら Repository で行う。
 * 中身の無い API は `ApiResult<Unit>` にする。
 * 失敗の側は `ApiResult<Nothing>` なので、どの `T` の結果にもそのまま入る。
 */
sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>

    /** HTTP 200 だが、本文の resultCode が成功（0）以外だった。 */
    data class BusinessError(val resultCode: Int) : ApiResult<Nothing>

    /**
     * 接続・読み込み・書き込み・呼び出し全体のどれかがタイムアウトした。
     * サーバが 408 Request Timeout / 504 Gateway Timeout を返したときもこれにする。
     */
    data object Timeout : ApiResult<Nothing>

    /** HTTP 4xx。サーバがエラー本文を返していれば [error] に入る。 */
    data class ClientError(val httpCode: Int, val error: ApiError? = null) : ApiResult<Nothing>

    /** HTTP 5xx。サーバがエラー本文を返していれば [error] に入る。 */
    data class ServerError(val httpCode: Int, val error: ApiError? = null) : ApiResult<Nothing>

    /** 圏外・接続拒否・切断など、タイムアウト以外の通信エラー。 */
    data object NetworkError : ApiResult<Nothing>

    /** JSON が読めないなど、上のどれでもない失敗。 */
    data class Unknown(val cause: Throwable) : ApiResult<Nothing>
}

/**
 * 4xx / 5xx のときにサーバが本文で返したエラーの内容。
 * 本文が空、または想定した JSON でなければ作らない（[ApiResult.ClientError.error] などが null になる）。
 */
data class ApiError(
    val code: String,
    val message: String,
)

/** サーバがエラー本文を返していればその内容。それ以外の結果では null。 */
val ApiResult<*>.apiError: ApiError?
    get() = when (this) {
        is ApiResult.ClientError -> error
        is ApiResult.ServerError -> error
        else -> null
    }
