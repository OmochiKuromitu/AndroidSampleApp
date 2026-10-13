package com.example.androidsampleapp.model

/**
 * 本文に結果コードを持つレスポンス。HTTP 200 でも、res が 0 以外なら失敗として扱う。
 *
 * ```json
 * { "res": 0, ... }
 * ```
 *
 * 項目名を JSON と同じ `res` にしている。@SerialName で別の名前に付け替えると、レスポンス型ごとに付け忘れが起きるため。
 *
 * レスポンス型（`@Serializable data class`）にこの interface を付けると、
 * Repository で `response.toApiResult()` と書いて判定できる。成功ならレスポンスがそのまま `Success` に入る。
 */
interface ResultCodeResponse {
    val res: Int

    companion object {
        const val SUCCESS = 0
    }
}
