package com.example.androidsampleapp.domain.model

/** 電話帳の 1 件。 */
data class Contact(
    val id: String,
    val name: String,
    val phoneNumber: String,
)

/** 通話履歴の 1 件。 */
data class CallHistory(
    val id: String,
    val name: String,
    /** 表示用に整形済みの日時。並べ替えはサーバ側で済んでいる前提。 */
    val occurredAt: String,
    val isMissed: Boolean,
)
