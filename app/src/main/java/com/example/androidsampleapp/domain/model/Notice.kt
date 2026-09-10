package com.example.androidsampleapp.domain.model

/**
 * 機器から受け取った通知。
 *
 * どの画面へ飛ぶかを通知自身が持つ。一覧側に「この分類ならここ」という
 * 対応表を書かずに済み、飛び先を増やすときも [NoticeDestination] だけで閉じる。
 */
data class Notice(
    val id: String,
    val category: NoticeCategory,
    val message: String,
    val destination: NoticeDestination,
)

/** 通知の分類。一覧ではタグとして色分けして出す。 */
enum class NoticeCategory(val label: String) {
    CALL("来客"),
    AIRCON("エアコン"),
    ALERT("警報"),
    INFO("お知らせ"),
    ;

    companion object {
        fun fromCode(code: String): NoticeCategory =
            entries.firstOrNull { it.name.equals(code, ignoreCase = true) } ?: INFO
    }
}

/**
 * 通知をタップしたときの飛び先。
 * ルート文字列との対応は ui 層（MainTab）が持ち、ドメインは画面の住所を知らない。
 */
enum class NoticeDestination {
    TOP,
    AIRCON,
    ;

    companion object {
        fun fromCode(code: String): NoticeDestination =
            entries.firstOrNull { it.name.equals(code, ignoreCase = true) } ?: TOP
    }
}
