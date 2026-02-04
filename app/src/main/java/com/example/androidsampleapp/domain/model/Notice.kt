package com.example.androidsampleapp.domain.model

/**
 * 一覧に出す通知。HTTP の API から取る。
 *
 * どの画面へ飛ぶかを通知自身が持つ。一覧側に「この分類ならここ」という
 * 対応表を書かずに済み、飛び先を増やすときも [NoticeDestination] だけで閉じる。
 */
data class Notice(
    val id: String,
    val category: NoticeCategory,
    /** 種別の横に出す短い見出し。無い通知もある。 */
    val title: String?,
    /** 見出しの下に出す詳細。 */
    val message: String,
    /** 起きた日時（epoch ミリ秒）。表示用の文字列ではなく比べられる値で持ち、整形は ui 層で行う。 */
    val occurredAt: Long,
    val destination: NoticeDestination,
)

/**
 * 通知の分類。一覧ではタグとして色分けして出す。
 * サーバの文字列との対応は data 層（data/CodeMapping）が、表示名は ui 層（ui/common/Labels）が持つ。
 */
enum class NoticeCategory {
    CALL,
    AIRCON,
    ALERT,
    INFO,
}

/**
 * 通知をタップしたときの飛び先。
 * ルート文字列との対応は ui 層が、サーバの文字列との対応は data 層（data/CodeMapping）が持つ。
 * ドメインは画面の住所も通信の言葉も知らない。
 *
 * enum ではなく sealed interface なのは、飛び先によって追加の情報を伴うものがあるため。
 */
sealed interface NoticeDestination {
    data object Top : NoticeDestination

    data object Aircon : NoticeDestination

    /**
     * 連絡先。
     *
     * [hasMissedCall] は「不在着信があった通知か」を表す。どのタブを開くかは ui 層の判断で、
     * ドメインは不在だったという事実だけを伝える。
     */
    data class Contact(val hasMissedCall: Boolean) : NoticeDestination
}
