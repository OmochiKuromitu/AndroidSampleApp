package com.example.androidsampleapp.network

import kotlinx.serialization.Serializable

/** サンプルのエラー仕様。実際のサーバの JSON に合わせて項目を変更する。 */
@Serializable
data class ApiErrorResponse(
    val code: String,
    val message: String,
)
