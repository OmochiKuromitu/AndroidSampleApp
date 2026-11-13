package com.example.androidsampleapp.config

/**
 * このアプリのバージョン。画面に出して、テスターさんの報告と突き合わせるのに使う。
 *
 * 値は BuildConfig に最初から入っている VERSION_NAME / VERSION_CODE。
 * コミット番号と日時はアプリに埋めない（毎回 git を呼ぶとビルドが遅くなるため）。APK のファイル名で確かめる。
 */
data class BuildInfo(
    /** バージョン名。versionNameSuffix（-mock など）まで含む。例: 1.0-mock */
    val versionName: String,
    /** バージョンコード。 */
    val versionCode: Int,
) {
    /** 1 行で出すとき。例: 1.0-mock (1) */
    val summary: String
        get() = "$versionName ($versionCode)"

    companion object {
        /** テストなど、BuildConfig を読まないときの値。 */
        val UNKNOWN = BuildInfo(versionName = "unknown", versionCode = 0)
    }
}
