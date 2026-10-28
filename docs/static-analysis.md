# 静的解析とカバレッジ

このリポジトリで使っている静的解析とカバレッジの道具、手元での実行方法、CI での動き、指摘が出たときの扱いをまとめる。

| 道具 | 何を見るか | 手元で実行 | レポート | 指摘で CI を止めるか |
| --- | --- | --- | --- | --- |
| JaCoCo | 単体テストがどの行・分岐を通ったか（カバレッジ） | `./gradlew jacocoMockDebugUnitTestReport` | `app/build/reports/jacoco/jacocoMockDebugUnitTestReport/html/index.html` | 止めない（テストの失敗では止まる） |
| Android Lint | Android 固有の問題（API レベル、リソース、マニフェスト、アクセシビリティなど） | `./gradlew lintMockDebug` | `app/build/reports/lint-results-mockDebug.html` | 止める（ベースラインにない error） |
| detekt | Kotlin のコードの書き方（複雑さ、命名、例外の扱いなど） | `./gradlew detekt` | `app/build/reports/detekt/detekt.html` | 止める（ベースラインにない指摘） |
| MobSF | 作った APK のセキュリティ（権限、通信、署名、埋め込まれた秘密情報など） | `scripts/mobsf-scan.sh`（下の「MobSF」） | `build/reports/mobsf/report.pdf` | 止めない（レポートを残すだけ） |

Gradle で動く 3 つをまとめて実行するなら次のとおり。

```sh
./gradlew lintMockDebug detekt jacocoMockDebugUnitTestReport
```

前提は JDK 17 と Android SDK（Android Studio を入れていれば揃っている）。MobSF だけは Docker が要る。

## 入れたもの

| ファイル | 内容 |
| --- | --- |
| `gradle/libs.versions.toml` | detekt と JaCoCo のバージョン、detekt の Gradle プラグイン |
| `build.gradle.kts` | detekt プラグインの宣言（`apply false`） |
| `app/build.gradle.kts` | detekt と JaCoCo の適用、Lint の設定、カバレッジのレポートを作るタスク |
| `config/detekt/detekt.yml` | detekt の設定（既定から変えたところだけ） |
| `config/detekt/baseline.xml` | detekt の既存の指摘 |
| `app/lint-baseline.xml` | Lint の既存の指摘（初回に作る。下の「Android Lint」） |
| `scripts/mobsf-scan.sh` | MobSF に APK を送ってレポートを保存するスクリプト |
| `.github/workflows/static-analysis.yml` | 上の 4 つを CI で動かすワークフロー |

## ベースラインの考え方

Lint と detekt は、入れた時点ですでにあるコードにも指摘を出す。全部を直してから入れると導入が進まないので、
既存の指摘はベースライン（指摘の一覧ファイル）に入れて無視し、**新しく増えた指摘だけでビルドを止める**。

- ベースラインに入っている指摘は、直したらベースラインからも消す（下の各節の「ベースラインを作り直す」）。
- 新しいコードの指摘をベースラインに足して黙らせることはしない。直すか、理由を書いて個別に抑制する。

## JaCoCo（カバレッジ）

### 何をしているか

- `android.buildTypes.debug.enableUnitTestCoverage = true` で、AGP が単体テストに JaCoCo のエージェントを付ける。
  実行結果は `app/build/outputs/unit_test_code_coverage/mockDebugUnitTest/testMockDebugUnitTest.exec` に出る。
- `android.testCoverage.jacocoVersion` と `jacoco.toolVersion` で、エージェントとレポートの JaCoCo のバージョンを揃えている。
- `jacocoMockDebugUnitTestReport` タスクが `testMockDebugUnitTest` を実行し、その結果から HTML と XML のレポートを作る。

### 実行

```sh
./gradlew jacocoMockDebugUnitTestReport
```

| レポート | 場所 |
| --- | --- |
| HTML（人が読む） | `app/build/reports/jacoco/jacocoMockDebugUnitTestReport/html/index.html` |
| XML（ほかの道具に読ませる） | `app/build/reports/jacoco/jacocoMockDebugUnitTestReport/jacocoMockDebugUnitTestReport.xml` |

### 集計から外しているもの

自動生成されたコードは、テストで直接触らないので数えると率が実態より下がって見える。
`app/build.gradle.kts` の `coverageExclusions` で外している。

- Android と Gradle が作るもの: `R`、`BuildConfig`、`Manifest`
- Hilt / Dagger が作るもの: `Hilt_*`、`*_Factory`、`*_MembersInjector`、`*_HiltModules` など
- Compose と kotlinx.serialization が作るもの: `ComposableSingletons`、`$serializer`

外すものを足すときは、このリストにパターンを足す。手で書いたコードは外さない（テストが無いことが見えなくなるため）。

### 下限を決めて CI を止めたいとき

今は率でビルドを止めていない。止めるなら `JacocoCoverageVerification` のタスクを足し、CI で実行する。

```kotlin
tasks.register<JacocoCoverageVerification>("jacocoMockDebugUnitTestVerification") {
    dependsOn("jacocoMockDebugUnitTestReport")
    val report = tasks.named<JacocoReport>("jacocoMockDebugUnitTestReport").get()
    classDirectories.setFrom(report.classDirectories)
    executionData.setFrom(report.executionData)
    violationRules {
        rule { limit { counter = "LINE"; minimum = "0.60".toBigDecimal() } }
    }
}
```

UI（Composable）は単体テストでほとんど通らないので、全体で下限を決めるより、
`domain` / `data` / Reducer のパッケージに絞って決めるほうが実態に合う。

## Android Lint

### 何をしているか

`app/build.gradle.kts` の `android.lint` で設定している。

| 設定 | 値 | 理由 |
| --- | --- | --- |
| `baseline` | `app/lint-baseline.xml` | 既存の指摘を無視する |
| `abortOnError` | `true` | ベースラインにない error でビルドを止める |
| `warningsAsErrors` | `false` | warning では止めない（レポートには出る） |
| `checkReleaseBuilds` | `false` | `assembleRelease` のたびに Lint を走らせない（CI の Lint ジョブで見る） |
| `htmlReport` / `xmlReport` / `sarifReport` | `true` | 人が読む用と、ほかの道具に読ませる用 |

### 初回：ベースラインを作る（1 回だけ）

`app/lint-baseline.xml` はまだリポジトリに入っていない。Lint はベースラインのファイルが無いと、
作ったうえで「作ったので確認して再実行して」と失敗する。初回は次のように実行して、できたファイルをコミットする。

```sh
./gradlew lintMockDebug -Dlint.baselines.continue=true
git add app/lint-baseline.xml
git commit -m "Android Lint のベースラインを作る"
```

CI はベースラインが無いときだけ同じ指定で実行し、できた `app/lint-baseline.xml` を成果物（`lint-report`）に入れる。
手元で作れないときは、CI の成果物からダウンロードしてコミットしてもよい。

### 実行

```sh
./gradlew lintMockDebug
```

レポートは `app/build/reports/lint-results-mockDebug.html`（ほかに `.xml` と `.sarif`）。

### 指摘が出たとき

1. まず直す。
2. 誤検知や、意図してそうしている場合は、その箇所だけ抑制し、理由をコメントで残す。
   - Kotlin: `@SuppressLint("ルールの ID")`
   - XML: `tools:ignore="ルールの ID"`
3. プロジェクト全体でそのルールを使わないと決めたら、`app/lint.xml` を作ってそこで無効にする（`android.lint.lintConfig` で指定する）。

### ベースラインを作り直す

既存の指摘を直したら、ベースラインからも消す。ファイルを消して作り直す。

```sh
rm app/lint-baseline.xml
./gradlew lintMockDebug -Dlint.baselines.continue=true
```

作り直すと、その時点で残っている指摘がすべて入る。新しく増えた指摘まで入っていないか、差分を見てからコミットする。

## detekt

### 何をしているか

- Gradle プラグイン `io.gitlab.arturbosch.detekt`（1.23.7）を `app` に適用している。
- 既定のルールの上に、`config/detekt/detekt.yml` で変えたいところだけを書いている（`buildUponDefaultConfig = true`）。
- 調べるのは `app/src/main/java` と `app/src/test/java`。
- レポートは HTML、XML、SARIF を出す。

`app/build.gradle.kts` の `configurations.matching { it.name == "detekt" }` は、detekt が使う Kotlin のバージョンを
detekt に合わせて戻すための設定。detekt 1.23.7 は Kotlin 2.0.10 で作られていて、
このプロジェクトの Kotlin（2.0.21）に引き上げられると起動時に失敗する。Kotlin や detekt のバージョンを上げても、この設定は残す。

### 設定で外しているもの

`config/detekt/detekt.yml` で外しているのは、Jetpack Compose の書き方に由来する誤検知だけ。

| ルール | 外し方 | 理由 |
| --- | --- | --- |
| `FunctionNaming` | `@Composable` を無視 | Composable は大文字で始める決まり |
| `TopLevelPropertyNaming` | 大文字始まりを許す | `ui/theme` の色（`TagCall` など） |
| `LongParameterList` / `LongMethod` | `@Composable` を無視 | Screen は操作ごとのコールバックを受け取るので引数が多い |
| `UnusedPrivateMember` | `@Preview` / `@PanelPreview` を無視 | プレビューは private で、Android Studio からしか呼ばれない |
| `MagicNumber` | プロパティの宣言と `ui/theme` を無視 | 色や寸法の定義は数値そのものが値 |

### ベースライン

`config/detekt/baseline.xml` に、導入した時点の指摘を入れてある（57 件。同じ箇所の重複をまとめて 41 項目）。
主なものは、HTTP のステータスコードや mock の応答の数値（`MagicNumber`）、
`catch (e: Exception)` で失敗をまとめて受けている箇所（`TooGenericExceptionCaught` / `SwallowedException`）、テストの長い行（`MaxLineLength`）。

### 実行

```sh
./gradlew detekt
```

レポートは `app/build/reports/detekt/detekt.html`（ほかに `.xml` と `.sarif`）。

`detektMockDebug` のようにビルドバリアント名の付いたタスクもあるが、使わない。
これは型の情報を使う分ルールが増える代わりに、ベースラインが別ファイルになり、実行も遅い。
使うなら、別にベースラインを作って CI に足す。

### 指摘が出たとき

1. まず直す。
2. 誤検知や、意図してそうしている場合は、その箇所だけ `@Suppress("ルールの ID")` で抑制し、理由をコメントで残す。
3. プロジェクト全体でそのルールの扱いを変えるなら、`config/detekt/detekt.yml` に書く。既定値は
   [default-detekt-config.yml](https://github.com/detekt/detekt/blob/v1.23.7/detekt-core/src/main/resources/default-detekt-config.yml) を見る。

### ベースラインを作り直す

```sh
./gradlew detektBaseline
```

その時点で残っている指摘でファイルが上書きされる。差分を見て、新しく増えた指摘が入っていないことを確かめてからコミットする。

## MobSF

### 何をしているか

MobSF（Mobile Security Framework）は、APK を解析してセキュリティ上の問題を報告する道具。Gradle ではなく、
Docker で動くサーバに APK を送って使う。`scripts/mobsf-scan.sh` が次の順に REST API を呼ぶ。

1. `api/v1/upload`：APK を送る
2. `api/v1/scan`：解析する（数分かかる）
3. `api/v1/report_json` / `api/v1/scorecard` / `api/v1/download_pdf`：結果を保存する

調べるのは配布する構成の `productRelease`。`mockDebug` は `debuggable` で、mock の通信の仕組みも入っているので、
配布するアプリとは違う指摘が出る。

### 手元で実行

Docker で MobSF を起動する。`MOBSF_API_KEY` は自分で決めた値でよい（スクリプトにも同じ値を渡す）。

```sh
docker run --rm -d --name mobsf -p 8000:8000 \
  -e MOBSF_API_KEY=local-mobsf-api-key \
  opensecurity/mobile-security-framework-mobsf:v4.5.3
```

APK を作ってスクリプトに渡す。`jq` と `curl` が要る。

```sh
./gradlew assembleProductRelease
MOBSF_API_KEY=local-mobsf-api-key \
  scripts/mobsf-scan.sh app/build/outputs/apk/product/release/app-product-release-unsigned.apk build/reports/mobsf
```

終わったら止める。

```sh
docker stop mobsf
```

ブラウザで `http://localhost:8000` を開くと、MobSF の画面からも結果を見られる（初期のユーザーとパスワードは `mobsf` / `mobsf`）。

### レポート

`build/reports/mobsf/` に出る。

| ファイル | 内容 |
| --- | --- |
| `report.pdf` | 人が読む用のレポート |
| `report.json` | 解析結果のすべて |
| `scorecard.json` | セキュリティスコア（0〜100）と、重要度ごとの指摘（`high` / `warning` / `info` / `secure` / `hotspot`） |

スクリプトの最後に、スコアと `high` / `warning` の件数を表示する。

### 指摘の扱い

MobSF の結果では CI を止めていない。配布前に `high` の指摘を確認し、直すか、問題ない理由を記録する。
止めたくなったら、`scripts/mobsf-scan.sh` の最後で `high` の件数を見て `exit 1` する。

### バージョンを上げる

結果が勝手に変わらないよう、ワークフローとこの文書で Docker イメージのバージョンを固定している（`v4.5.3`）。
上げるときは、両方の `opensecurity/mobile-security-framework-mobsf:v4.5.3` を書き換え、
[MobSF のリリースノート](https://github.com/MobSF/Mobile-Security-Framework-MobSF/releases)で API の変更がないか確かめる。

## CI（GitHub Actions）

`.github/workflows/static-analysis.yml` が、Pull Request と `develop` / `main` への push で動く。
Actions の画面から手で実行することもできる（`workflow_dispatch`）。

| ジョブ | 実行するもの | 成果物 |
| --- | --- | --- |
| Android Lint | `./gradlew lintMockDebug` | `lint-report` |
| detekt | `./gradlew detekt` | `detekt-report` |
| 単体テストと JaCoCo | `./gradlew jacocoMockDebugUnitTestReport` | `coverage-report`（カバレッジとテスト結果） |
| MobSF | `./gradlew assembleProductRelease` のあと `scripts/mobsf-scan.sh` | `mobsf-report` |

成果物は、実行結果の画面の下にある Artifacts からダウンロードできる。失敗したときも出す（`if: always()`）。

MobSF のジョブは、MobSF を同じジョブのサービスコンテナとして起動する。API キーは固定値（`ci-mobsf-api-key`）にしている。
このコンテナはジョブの中からしか届かず、終われば捨てられるので、秘密にする必要がない。

## 道具を足す・バージョンを上げるとき

- バージョンは `gradle/libs.versions.toml` にまとめてある（`detekt`、`jacoco`）。
- detekt を上げたら、`./gradlew detekt` を一度実行し、新しいルールで指摘が増えていないか確かめる。
  増えていたら直すか、`config/detekt/detekt.yml` で扱いを決める。ベースラインに足して黙らせない。
- AGP を上げたら Lint のルールも増える。`./gradlew lintMockDebug` を実行し、同じように扱いを決める。
