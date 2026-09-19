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
    /** 無い通知もある。 */
    val title: String?,
    val message: String,
    /** epoch ミリ秒。TODO: API の仕様が決まったら形式を合わせる。 */
    val occurredAt: Long,
    val destination: String,
)
