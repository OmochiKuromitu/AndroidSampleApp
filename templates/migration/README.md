# 移植先用の tester-build.yml

移植先のプロジェクト（flavorDimensions が `mode` → `environment` の 2 つ）向けの、テスター配布のワークフロー。
このリポジトリでは動かさないので、`.github/workflows/` ではなくここに置いている。

## 使い方

1. `tester-build.yml` を、移植先のリポジトリの `.github/workflows/tester-build.yml` にコピーする
2. mode と environment の flavor がほかにもあれば、`options` の行に足す（コメントの付いている行）
3. 移植先の `app/build.gradle.kts` に、release の署名の設定を足す（下）
4. Secrets と Variables、Google Cloud、共有ドライブ、Slack を準備する（このリポジトリの `docs/tester-build.md` と同じ）

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
| ファイル名 | `AndroidSampleApp-<flavor>-<ビルドタイプ>-<実行番号>-<コミット>.apk` | `App-<ビルドバリアント>-<実行番号>-<コミット>.apk` |
| Slack の文面 | 構成: product / release | ビルドバリアント: mockProdRelease |

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
