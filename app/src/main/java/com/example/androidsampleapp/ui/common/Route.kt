package com.example.androidsampleapp.ui.common

/**
 * 画面の住所。文字列リテラルを散らかさないよう、ここ 1 か所で定義する。
 */
object Route {
    /** 下部バーを持つメイン画面。 */
    const val MAIN = "main"

    /** スリープ画面。全画面表示のため、メインとは別のトップレベルルートにする。 */
    const val SLEEP = "sleep"

    /** メイン画面の中のタブ。 */
    const val TOP = "main/top"
    const val AIRCON = "main/aircon"
}
