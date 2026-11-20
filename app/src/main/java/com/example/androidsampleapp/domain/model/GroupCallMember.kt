package com.example.androidsampleapp.domain.model

/**
 * グループ通話に参加している相手。
 *
 * 画面を仮置きした段階のモデル。通話の API が決まったら、そのレスポンスに合わせて項目を見直す。
 */
data class GroupCallMember(
    /** 一覧のキー。メンバーの出入りで一覧が並び替わっても、同じ人の行を使い回せるようにする。 */
    val id: String,
    /** 相手の名前。例: 1号車運転台 */
    val name: String,
    /** 相手の端末の種類。例: ハンドセット */
    val device: String,
)
