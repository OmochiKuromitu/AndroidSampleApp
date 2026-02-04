package com.example.androidsampleapp.ui.common

/**
 * 画面の住所。文字列リテラルを散らかさないよう、ここ 1 か所で定義する。
 * これらを使って実際に遷移するのは AppNavigation だけ。
 */
object Route {
    const val TOP = "top"
    const val AIRCON = "aircon"
    const val CONTACT = "contact"

    /** スリープ画面。下部バーを出さないので、タブの枠には入れない。 */
    const val SLEEP = "sleep"

    /** 連絡先で最初に開くリストを渡す引数の名前。 */
    const val ARG_CONTACT_LIST = "list"

    /** 履歴から開きたいときの引数の値。既定（電話帳）のときは引数を付けない。 */
    const val CONTACT_LIST_HISTORY = "history"

    /**
     * 連絡先を NavHost に登録するときのパターン。引数は省略でき、
     * 省略時は既定のリスト（電話帳）で開く。
     */
    const val CONTACT_PATTERN = "$CONTACT?$ARG_CONTACT_LIST={$ARG_CONTACT_LIST}"

    /** 連絡先への行き先を作る。履歴から開きたいときだけ引数を付ける。 */
    fun contact(showHistory: Boolean): String =
        if (showHistory) "$CONTACT?$ARG_CONTACT_LIST=$CONTACT_LIST_HISTORY" else CONTACT
}
