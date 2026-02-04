package com.example.androidsampleapp.model

import kotlinx.serialization.Serializable

/**
 * 通知取得 API のレスポンス 1 件分。`POST notices/list` は、これの JSON 配列を返す。
 *
 * サーバの語彙のまま受け取り、ドメインの [com.example.androidsampleapp.domain.model.Notice]
 * への変換は data 層が行う。API の項目名が変わっても影響をこの型と変換に閉じ込める。
 *
 * 形はサンプル用の仮の仕様。
 */
@Serializable
data class NoticeResponse(
    val id: String,
    val category: String,
    /** 無い通知もある。項目ごと省かれても読めるよう、既定値を置く。 */
    val title: String? = null,
    val message: String,
    /** epoch ミリ秒。 */
    val occurredAt: Long,
    val destination: String,
)
