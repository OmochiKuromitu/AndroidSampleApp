package com.example.androidsampleapp.model

import kotlinx.serialization.Serializable

/**
 * 電話帳取得 API（`contacts/list`）のレスポンス 1 件分。API はこれの JSON 配列を返す。
 *
 * サーバの語彙のまま受け取り、ドメインへの変換は data 層が行う。
 */
@Serializable
data class ContactResponse(
    val id: String,
    val name: String,
    val phoneNumber: String,
)

/**
 * 通話履歴取得 API（`calls/history`）のレスポンス 1 件分。API はこれの JSON 配列を返す（新しい順）。
 */
@Serializable
data class CallHistoryResponse(
    val id: String,
    val name: String,
    /** 表示用に整形済みの日時（「9月11日 14:32」）。サーバが整形して返す仮の仕様。 */
    val occurredAt: String,
    /** 応答しなかった着信か。 */
    val missed: Boolean,
)

/** 不在着信件数 API（`missed-calls/count`）のレスポンス。 */
@Serializable
data class MissedCallCountResponse(
    /** 未確認の不在着信の件数。 */
    val count: Int,
)
