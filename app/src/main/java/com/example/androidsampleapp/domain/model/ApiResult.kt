package com.example.androidsampleapp.domain.model

/**
 * API を 1 回呼んだ結果。Repository が返し、ViewModel は when で出し分ける。
 *
 * 例外を投げずに戻り値で失敗を返すので、呼ぶ側に try / catch は要らない。
 * キャンセルだけは結果にせず、例外のまま伝える（画面を離れたあとに失敗の表示を出さないため）。
 *
 * [Success] の中身は Retrofit のレスポンス型ではなく、変換後のドメインの型を入れる。
 * 中身の無い API は `ApiResult<Unit>` にする。
 * 失敗の側は `ApiResult<Nothing>` なので、どの `T` の結果にもそのまま入る。
 */
sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>

    /** HTTP 200 だが、本文の resultCode が成功（0）以外だった。 */
    data class BusinessError(val resultCode: Int) : ApiResult<Nothing>

    /** 接続・読み込み・呼び出し全体のどれかがタイムアウトした。 */
    data object Timeout : ApiResult<Nothing>

    /** HTTP 4xx。 */
    data class ClientError(val httpCode: Int) : ApiResult<Nothing>

    /** HTTP 5xx。 */
    data class ServerError(val httpCode: Int) : ApiResult<Nothing>

    /** 圏外・接続拒否・切断など、タイムアウト以外の通信エラー。 */
    data object NetworkError : ApiResult<Nothing>

    /** JSON が読めないなど、上のどれでもない失敗。 */
    data class Unknown(val cause: Throwable) : ApiResult<Nothing>
}
