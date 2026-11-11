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

## 動くタイミング

手で実行するときだけ動く（Actions の画面の Run workflow、または `gh workflow run`）。push では動かない。
Run workflow のボタンは、ワークフローのファイルが移植先の既定のブランチに入っているときだけ表示される。

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
| 画面で選ぶときの初期値 | product / release | mock / prod / release |
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

設定画面には、たとえば次のように出す（`BuildConfig` はアプリのパッケージのもの）。

```kotlin
@Composable
fun BuildInfoText(modifier: Modifier = Modifier) {
    Text(
        // 例: 1.0-mock (1) / a1b2c3d / 2026-11-11 14:30
        text = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) / " +
            "${BuildConfig.BUILD_COMMIT} / ${BuildConfig.BUILD_COMMIT_DATE}",
        style = MaterialTheme.typography.bodySmall,
        modifier = modifier,
    )
}
```

移植先に、BuildConfig を直接読まずに設定のクラスを通す決まりがあるなら、そちらに値を移す
（このリポジトリでは `config/BuildInfo.kt` と `AppConfig.buildInfo`）。

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
