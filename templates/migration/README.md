# 移植先用の tester-build.yml

移植先のプロジェクト（flavorDimensions が `mode` → `environment` の 2 つ）向けの、テスター配布のワークフロー。
このリポジトリでは動かさないので、`.github/workflows/` ではなくここに置いている。

## 使い方

1. `tester-build.yml` を、移植先のリポジトリの `.github/workflows/tester-build.yml` にコピーする
2. mode と environment の flavor がほかにもあれば、`options` の行に足す（コメントの付いている行）
3. 移植先の `app/build.gradle.kts` に、release の署名の設定と、コミットの情報を BuildConfig に埋める設定を足す（下）
4. Secrets と Variables、Google Cloud、共有ドライブ、Slack を準備する（このリポジトリの `docs/tester-build.md` と同じ）

ワークフローは JDK 21 で Gradle を動かす（`setup-java` の `java-version: 21`）。移植先の AGP と Gradle が JDK 21 に対応している必要がある
（AGP 8.2 以降、Gradle 8.5 以降）。ローカルも揃えるなら、移植先に `gradle/gradle-daemon-jvm.properties` を置いて `toolchainVersion=21` と書く
（Gradle 8.8 以降）。

GCP の準備が済むまでは、リポジトリ変数 `DRIVE_FOLDER_ID` を設定しないでおく。その間は認証とドライブへのアップロードを飛ばし、
APK は Actions の成果物（Artifacts）にだけ置く。Slack には Actions の実行結果へのリンクを送る。
`DRIVE_FOLDER_ID` を設定すれば、ワークフローを触らずにドライブへのアップロードが動き出す
（そのときは `GCP_WORKLOAD_IDENTITY_PROVIDER` と `GCP_SERVICE_ACCOUNT` も一緒に設定する）。

## ビルドバリアントの決まり

flavorDimensions の順（mode → environment）に flavor 名を並べ、最後にビルドタイプを付ける。2 つ目からは頭を大文字にする。

| mode | environment | ビルドタイプ | ビルドバリアント | タスク |
| --- | --- | --- | --- | --- |
| mock | prod | release | `mockProdRelease` | `assembleMockProdRelease` |
| mock | prod | debug | `mockProdDebug` | `assembleMockProdDebug` |

APK は出力先のフォルダ名を組み立てずに `app/build/outputs/apk` の下を探すので、flavor が増えてもワークフローは直さなくてよい。
APK を `app` 以外のモジュールで作っているなら、`find app/build/outputs/apk` の `app` を変える。

## このリポジトリ版との違い

| 項目 | このリポジトリ版 | 移植先版 |
| --- | --- | --- |
| 手で実行するときに選ぶもの | flavor、ビルドタイプ | mode、environment、ビルドタイプ |
| push のときの既定を決めるリポジトリ変数 | `TESTER_BUILD_FLAVOR` / `TESTER_BUILD_TYPE` | `TESTER_BUILD_MODE` / `TESTER_BUILD_ENVIRONMENT` / `TESTER_BUILD_TYPE` |
| 既定値（変数が無いとき） | product / release | mock / prod / release |
| ファイル名 | `AndroidSampleApp-<ビルドバリアント>-<バージョン>-<コミット>-<コミットの日時>-r<実行番号>.apk` | `App-<ビルドバリアント>-<バージョン>-<コミット>-<コミットの日時>-r<実行番号>.apk` |
| Slack の文面 | 構成: product / release | ビルドバリアント: mockProdRelease |

## APK のファイル名

`App-<ビルドバリアント>-<バージョン>-<コミット>-<コミットの日時>-r<実行番号>.apk`

例: `App-mockProdRelease-1.0-mock-a1b2c3d-20261111-1430-r12.apk`

| 部分 | 例 | 出どころ |
| --- | --- | --- |
| ビルドバリアント | `mockProdRelease` | 選んだ mode、environment、ビルドタイプ |
| バージョン | `1.0-mock` | できた APK の中の versionName（`aapt2` で読む。versionNameSuffix まで含むので、アプリの表示と必ず揃う） |
| コミット | `a1b2c3d` | ビルドしたコミットの先頭 7 文字 |
| コミットの日時 | `20261111-1430` | ビルドしたコミットの日時（日本時間） |
| 実行番号 | `r12` | Actions の実行一覧の `#12` |

先頭の `App` は、ワークフローの「APK を作る」の `name=` の行で変えられる。

## アプリの中でバージョンとコミットを確かめる（移植先の app/build.gradle.kts とアプリ）

`BuildConfig` には、バージョン名（`VERSION_NAME`）とバージョンコード（`VERSION_CODE`）が最初から入っている。
コミット番号と日時は入らないので、`buildConfigField` で足す。値はワークフローが `-PbuildCommit` / `-PbuildCommitDate` で渡すので、
APK のファイル名と同じ値になる。手元のビルドでは git の HEAD から読む。

```kotlin
// ビルドしたコミットの番号と日時。CI は -PbuildCommit / -PbuildCommitDate で渡す。
// 渡されないとき（手元のビルド）は git から読む。git が使えなければ "unknown"。
fun gitOutput(vararg args: String): String? = runCatching {
    providers.exec {
        commandLine("git", *args)
        isIgnoreExitValue = true
    }.standardOutput.asText.get().trim().ifEmpty { null }
}.getOrNull()

val buildCommit: String = providers.gradleProperty("buildCommit").orNull
    ?: gitOutput("rev-parse", "--short=7", "HEAD")
    ?: "unknown"
val buildCommitDate: String = providers.gradleProperty("buildCommitDate").orNull
    ?: gitOutput("log", "-1", "--format=%cd", "--date=format-local:%Y-%m-%d %H:%M")
    ?: "unknown"

android {
    buildFeatures {
        buildConfig = true   // AGP 8 から既定で無効なので、有効にしておく
    }
    defaultConfig {
        buildConfigField("String", "BUILD_COMMIT", "\"$buildCommit\"")
        buildConfigField("String", "BUILD_COMMIT_DATE", "\"$buildCommitDate\"")
    }
}
```

画面からは、`BuildConfig` を直接読まずに `BuildInfo` を通して読む。`BuildInfo` は app モジュールの main
（`src/main/java`）に 1 つだけ置けばよい。`BuildConfig` がビルドバリアントごとに作り分けられるので、
同じコードのまま、ビルドした構成の値が入る。flavor ごとのフォルダ（`src/mock/java` など）に書き分ける必要はない。

```kotlin
package com.example.yourapp.config   // 置く場所は移植先に合わせる

import com.example.yourapp.BuildConfig   // アプリ（app モジュール）の BuildConfig

/**
 * このアプリをどのコードからビルドしたか。設定画面に出して、テスターさんの報告と突き合わせる。
 *
 * 値は BuildConfig から読む。BuildConfig はビルドバリアントごとに作られるので、
 * flavor ごとにこのクラスを書き分けなくてよい。
 */
data class BuildInfo(
    val versionName: String,
    val versionCode: Int,
    val commit: String,
    val commitDate: String,
    /** ビルドバリアント。例: mockProdRelease */
    val variant: String,
) {
    /** 1 行で出すとき。例: 1.0-mock (1) / mockProdRelease / a1b2c3d / 2026-11-11 14:30 */
    val summary: String
        get() = "$versionName ($versionCode) / $variant / $commit / $commitDate"

    companion object {
        /** このビルドの値。 */
        val current = BuildInfo(
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE,
            commit = BuildConfig.BUILD_COMMIT,
            commitDate = BuildConfig.BUILD_COMMIT_DATE,
            // FLAVOR は dimension の順に並んだ名前（mockProd）。最後にビルドタイプを付ける。
            variant = BuildConfig.FLAVOR + BuildConfig.BUILD_TYPE.replaceFirstChar { it.uppercase() },
        )
    }
}
```

設定画面では、`BuildInfo.current.summary` を出すだけ。

```kotlin
Text(
    // 例: 1.0-mock (1) / mockProdRelease / a1b2c3d / 2026-11-11 14:30
    text = BuildInfo.current.summary,
    style = MaterialTheme.typography.bodySmall,
)
```

プレビューやテストでは `BuildInfo.current` を使わず、`BuildInfo(versionName = "1.0-mock", ...)` と値を直接入れて作る。
ビルドの値に左右されずに表示を確かめられる。

`BuildInfo` は app モジュールに置く。ライブラリのモジュール（`:core` など）にもそのモジュール用の `BuildConfig` は作られるが、
そちらには `VERSION_NAME` / `VERSION_CODE` が入らず、app の `build.gradle.kts` で足した `BUILD_COMMIT` も見えない。
別モジュールの画面に出したいときは、app 側で `BuildInfo.current` を作って画面に渡す。

## release の署名の設定（移植先の app/build.gradle.kts）

ワークフローは、release のときだけ鍵を環境変数で渡す。Gradle 側では、渡されたときだけ release に署名する。

```kotlin
// release の署名に使う鍵。CI が環境変数で渡したときだけ署名する。
// 渡されないときは署名なしの APK になる（手元のビルドなど）。
val releaseStoreFile: String? = providers.environmentVariable("RELEASE_STORE_FILE").orNull

android {
    signingConfigs {
        if (releaseStoreFile != null) {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = providers.environmentVariable("RELEASE_STORE_PASSWORD").get()
                keyAlias = providers.environmentVariable("RELEASE_KEY_ALIAS").get()
                keyPassword = providers.environmentVariable("RELEASE_KEY_PASSWORD").get()
            }
        }
    }

    buildTypes {
        release {
            if (releaseStoreFile != null) signingConfig = signingConfigs.getByName("release")
            // ...移植先のもとの設定
        }
    }
}
```

移植先にすでに release の `signingConfig` がある場合は、それを生かし、CI から鍵を渡す方法だけ合わせる。
