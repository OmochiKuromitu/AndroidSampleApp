package com.example.androidsampleapp.ui.common

/**
 * 画面の住所。文字列リテラルを散らかさないよう、ここ 1 か所で定義する。
 * これらを使って実際に遷移するのは AppNavigation だけ。
 */
object Route {
    const val TOP = "top"
    const val AIRCON = "aircon"

    /** スリープ画面。下部バーを出さないので、タブの枠には入れない。 */
    const val SLEEP = "sleep"
}
