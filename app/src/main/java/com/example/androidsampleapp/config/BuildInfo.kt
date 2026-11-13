package com.example.androidsampleapp.config

/**
 * このアプリをどのコードからビルドしたか。設定画面などに出して、テスターさんの報告と突き合わせるのに使う。
 *
 * 値は app/build.gradle.kts が BuildConfig に埋める。CI（tester-build）のビルドなら、APK のファイル名と同じ値になる。
 * 手元のビルドでは、ビルドしたときの git の HEAD から読む（読めなければ "unknown"）。
 */
data class BuildInfo(
    /** バージョン名。versionNameSuffix（-mock など）まで含む。例: 1.0-mock */
    val versionName: String,
    /** バージョンコード。 */
    val versionCode: Int,
    /** コミット番号（先頭 7 文字）。例: a1b2c3d */
    val commit: String,
    /** コミットの日時。CI のビルドは日本時間。例: 2026-11-11 14:30 */
    val commitDate: String,
) {
    /** 1 行で出すとき。例: 1.0-mock (1) / a1b2c3d / 2026-11-11 14:30 */
    val summary: String
        get() = "$versionName ($versionCode) / $commit / $commitDate"

    companion object {
        /** テストなど、BuildConfig を読まないときの値。 */
        val UNKNOWN = BuildInfo(versionName = "unknown", versionCode = 0, commit = "unknown", commitDate = "unknown")
    }
}
