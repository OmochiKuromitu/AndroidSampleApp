package com.example.androidsampleapp.model

/**
 * 電話帳取得 API のレスポンス 1 件分。
 */
data class ContactResponse(
    val id: String,
    val name: String,
    val phoneNumber: String,
)

/**
 * 通話履歴取得 API のレスポンス 1 件分。
 */
data class CallHistoryResponse(
    val id: String,
    val name: String,
    val occurredAt: String,
    val missed: Boolean,
)
