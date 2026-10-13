package com.example.androidsampleapp.model

/**
 * 本文に結果コードを持つレスポンス。HTTP 200 でも、resultCode が 0 以外なら失敗として扱う。
 *
 * レスポンス型（`@Serializable data class`）にこの interface を付けると、
 * Repository で `response.toApiResult { ... }` と書いて判定できる。
 */
interface ResultCodeResponse {
    val resultCode: Int

    companion object {
        const val SUCCESS = 0
    }
}
