package com.example.androidsampleapp.model

import kotlinx.serialization.Serializable

/**
 * 4xx / 5xx のときにサーバが返すエラーの本文。サンプル用の仮の仕様なので、使うサーバの JSON に合わせて項目を直す。
 *
 * ```json
 * { "code": "INVALID_DEVICE", "message": "端末が不正です" }
 * ```
 */
@Serializable
data class ApiErrorResponse(
    val code: String,
    val message: String,
)
