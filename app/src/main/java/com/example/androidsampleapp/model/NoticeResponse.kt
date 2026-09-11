package com.example.androidsampleapp.model

/**
 * 通知取得 API のレスポンス 1 件分。
 *
 * サーバの語彙のまま受け取り、ドメインの [com.example.androidsampleapp.domain.model.Notice]
 * への変換は data 層が行う。API の項目名が変わっても影響をこの型と変換に閉じ込める。
 */
data class NoticeResponse(
    val id: String,
    val category: String,
    val message: String,
    val destination: String,
)
