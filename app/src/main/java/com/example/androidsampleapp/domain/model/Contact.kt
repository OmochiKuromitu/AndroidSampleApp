package com.example.androidsampleapp.domain.model

/**
 * 電話帳と通話履歴のひとまとまり。
 *
 * 画面は両方そろってから出す（片方だけ出すことはしない）ので、取得も保持もまとめて扱う。
 */
data class AddressBook(
    val contacts: List<Contact>,
    /** 新しいものが先頭。 */
    val histories: List<CallHistory>,
)

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
