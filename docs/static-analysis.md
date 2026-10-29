# 静的解析とカバレッジ

このリポジトリで使っている静的解析とカバレッジの道具、手元での実行方法、指摘が出たときの扱いをまとめる。
解析はすべて手元で実行する。GitHub Actions には登録していない（下の「GitHub Actions」）。

| 道具 | 何を見るか | 手元で実行 | レポート | 指摘で失敗にするか |
| --- | --- | --- | --- | --- |
| JaCoCo | 単体テストがどの行・分岐を通ったか（カバレッジ） | `./gradlew jacocoMockDebugUnitTestReport` | `app/build/reports/jacoco/jacocoMockDebugUnitTestReport/html/index.html` | しない（テストの失敗では失敗する） |
| Android Lint | Android 固有の問題（API レベル、リソース、マニフェスト、アクセシビリティなど） | `./gradlew lintMockDebug` | `app/build/reports/lint-results-mockDebug.html` | する（ベースラインにない error） |
| detekt | Kotlin のコードの書き方（複雑さ、命名、例外の扱いなど） | `./gradlew detekt` | `app/build/reports/detekt/detekt.html` | する（ベースラインにない指摘） |
| MobSF | 作った APK のセキュリティ（権限、通信、署名、埋め込まれた秘密情報など） | `scripts/mobsf-scan.ps1`（下の「MobSF」） | `build/reports/mobsf/<実行日時>/report.pdf` | しない（レポートを残すだけ） |

前提は JDK 17 と Android SDK（Android Studio を入れていれば揃っている）。MobSF だけは Podman が要る。

### まとめて実行する（Windows）

PowerShell 7（`pwsh`）で、リポジトリのルートから実行する。MobSF は先に起動しておく（下の「MobSF」の 1 と 2）。

```powershell
$env:MOBSF_API_KEY = 'local-mobsf-api-key'
pwsh -File scripts/static-analysis.ps1
```

`scripts/static-analysis.ps1` は次の順に実行する。

1. Lint、detekt、単体テストと JaCoCo を `--continue` で実行する（どれかが失敗しても残りは続ける）
2. `productRelease` の APK を作り、`scripts/mobsf-scan.ps1` で MobSF に送る（APK を作れなかったら、古い APK は送らない）

どれかが失敗していたら、最後に失敗したものを並べて終了コード 1 で終わる。全部通れば「指定した解析が完了しました。」と出る。

MobSF をまだ用意していないときは、Gradle の 3 つだけを実行できる。

```powershell
pwsh -File scripts/static-analysis.ps1 -SkipMobSF
```

Mac / Linux では Gradle の 3 つをまとめて実行し、MobSF は `scripts/mobsf-scan.sh` を使う（下の「MobSF」）。

```sh
./gradlew lintMockDebug detekt jacocoMockDebugUnitTestReport --continue
```

## 入れたもの

| ファイル | 内容 |
| --- | --- |
| `gradle/libs.versions.toml` | detekt と JaCoCo のバージョン、detekt の Gradle プラグイン |
| `build.gradle.kts` | detekt プラグインの宣言（`apply false`） |
| `app/build.gradle.kts` | detekt と JaCoCo の適用、Lint の設定、カバレッジのレポートを作るタスク |
| `config/detekt/detekt.yml` | detekt の設定（既定から変えたところだけ） |
| `config/detekt/baseline.xml` | detekt の既存の指摘 |
| `app/lint-baseline.xml` | Lint の既存の指摘（初回に作る。下の「Android Lint」） |
| `scripts/static-analysis.ps1` | 上の 4 つを手元でまとめて実行するスクリプト（Windows） |
| `scripts/mobsf-scan.ps1` | MobSF に APK を送ってレポートを保存するスクリプト（Windows） |
| `scripts/mobsf-scan.sh` | 同じことをする Bash 版（Mac / Linux。`curl` と `jq` が要る） |

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

### 下限を決めて失敗にしたいとき

今は率でビルドを止めていない。止めるなら `JacocoCoverageVerification` のタスクを足し、`scripts/static-analysis.ps1` の Gradle のタスクに加える。

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
| `checkReleaseBuilds` | `false` | `assembleRelease` のたびに Lint を走らせない（`lintMockDebug` で見る） |
| `htmlReport` / `xmlReport` / `sarifReport` | `true` | 人が読む用と、ほかの道具に読ませる用 |

### 初回：ベースラインを作る（1 回だけ）

Lint はベースラインのファイルが無いと、作ったうえで「作ったので確認して再実行して」と失敗する
（`Aborting build since new baseline file was created`）。初回は次のように実行して、できたファイルの中身を確かめてからコミットする。
`scripts/static-analysis.ps1` を先に実行してこのメッセージで失敗した場合も、ファイルはできているので、確かめてコミットし、もう一度実行する。

```sh
./gradlew lintMockDebug -Dlint.baselines.continue=true
git add app/lint-baseline.xml
git commit -m "Android Lint のベースラインを作る"
```

PowerShell では `-D` の引数を `'-Dlint.baselines.continue=true'` のように引用符で囲む。

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
使うなら、別にベースラインを作って `scripts/static-analysis.ps1` に足す。

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
コンテナで動くサーバに APK を送って使う。このリポジトリではコンテナを Podman で動かす。
`scripts/mobsf-scan.ps1`（Bash 版は `scripts/mobsf-scan.sh`）が次の順に REST API を呼ぶ（スクリプトはコンテナの操作はしない）。

1. `api/v1/upload`：APK を送る
2. `api/v1/scan`：解析する（数分かかる）
3. `api/v1/report_json` / `api/v1/scorecard` / `api/v1/download_pdf`：結果を保存する

調べるのは配布する構成の `productRelease`。`mockDebug` は `debuggable` で、mock の通信の仕組みも入っているので、
配布するアプリとは違う指摘が出る。

### 手元で実行

#### 1. Podman を用意する（初回だけ）

[Podman のインストール手順](https://podman.io/docs/installation)に従って入れる。Windows なら winget で入る。

```powershell
winget install -e --id RedHat.Podman --source winget
```

Windows の Podman は WSL の上で動く。WSL が古い（2.5 より前）と、仮想マシンが起動しない（下の「うまく動かないとき」）。
先に `wsl --update` で上げておく。

Mac と Windows では、Podman は Linux の仮想マシンの中でコンテナを動かす。最初に仮想マシンを作っておく。

```sh
podman machine init
```

Mac と Windows は、使う前に毎回（PC を起動したあとなど）仮想マシンを起動する。Linux ではこの手順は要らない。

```sh
podman machine start
```

#### 2. MobSF を起動する

`MOBSF_API_KEY` は自分で決めた値でよい（スクリプトにも同じ値を渡す）。外部で登録したり発行してもらったりするものではない。

```powershell
podman run --rm -d --name mobsf -p 127.0.0.1:8000:8000 -e MOBSF_API_KEY=local-mobsf-api-key docker.io/opensecurity/mobile-security-framework-mobsf:v4.5.3
```

`-p 127.0.0.1:8000:8000` で、この PC からしか届かないようにしている。API キーが決め打ちの値なので、LAN に公開しない。

イメージ名は `docker.io/` から書く。Podman は短い名前（`opensecurity/...`）だと、どのレジストリから取るかを
設定（`registries.conf`）で決めるので、環境によっては選択を聞かれたり、取得に失敗したりする。

`-e MOBSF_API_KEY` を付けずに起動した場合は、MobSF が作ったキーが起動ログに出る。

```sh
podman logs mobsf 2>&1 | grep "REST API Key"
```

#### 3. APK を解析する

`scripts/static-analysis.ps1` を使うなら、この手順はスクリプトがやる。MobSF だけを実行するときは、APK を作ってスクリプトに渡す。

```powershell
.\gradlew.bat assembleProductRelease
$env:MOBSF_API_KEY = 'local-mobsf-api-key'
pwsh -File scripts/mobsf-scan.ps1 -ApkPath app/build/outputs/apk/product/release/app-product-release-unsigned.apk
```

Mac / Linux では Bash 版を使う。`jq` と `curl` が要る。

```sh
./gradlew assembleProductRelease
MOBSF_API_KEY=local-mobsf-api-key \
  scripts/mobsf-scan.sh app/build/outputs/apk/product/release/app-product-release-unsigned.apk build/reports/mobsf
```

接続先は既定で `http://localhost:8000`。変えるときは環境変数 `MOBSF_URL` を設定する（どちらのスクリプトも同じ）。

#### 4. 止める

```sh
podman stop mobsf
```

`--rm` を付けて起動しているので、止めるとコンテナも消える。解析結果は `build/reports/mobsf/` に保存済み。

#### うまく動かないとき

| 症状 | 確かめること |
| --- | --- |
| `Cannot connect to Podman` と出る（Mac / Windows） | `podman machine start` で仮想マシンを起動したか |
| `podman machine start` が `machine did not transition into running state` で失敗する（Windows） | `wsl --version` を見る。WSL 2.5 より前（カーネル 5.15）は cgroup v1 を使っていて、Podman の仮想マシンの systemd（v258 以降は cgroup v1 に対応しない）が起動できない。`wsl --update` のあと `wsl --shutdown` して、もう一度 `podman machine start` |
| `podman machine start` で `\\.\pipe\docker_engine: All pipe instances are busy` と出る（Windows） | Docker Desktop が同じ名前のパイプを使っているだけで、Podman はそのまま使える。逆に Docker Desktop より先に Podman を起動すると、`docker` コマンドが Podman につながる |
| スクリプトが「起動しませんでした」で止まる | `podman logs mobsf` で MobSF のログを見る。初回はイメージの取得と起動に数分かかるので、待つ時間を延ばす（`mobsf-scan.ps1` は `-WaitSeconds 600`、`mobsf-scan.sh` は `MOBSF_WAIT_SEC=600`） |
| `-v` でホストのディレクトリをマウントすると書き込めない | Podman を一般ユーザー（root なし）で動かしているとき、コンテナの中のユーザーに書き込む権限がないことがある。SELinux が有効な Fedora や RHEL では `-v ./mobsf-data:/home/mobsf/.MobSF:Z` のように `:Z` も付ける。上の手順のようにマウントしなければ起きない |

Docker でも、上のコマンドの `podman` を `docker` に置き換えれば同じように動く。

ブラウザで `http://localhost:8000` を開くと、MobSF の画面からも結果を見られる（初期のユーザーとパスワードは `mobsf` / `mobsf`）。

### レポート

`mobsf-scan.ps1` は `build/reports/mobsf/<実行日時>/` に、実行ごとに分けて出す（前の結果と混ざらないように）。
`mobsf-scan.sh` は `build/reports/mobsf/` に上書きで出す。

| ファイル | 内容 |
| --- | --- |
| `report.pdf` | 人が読む用のレポート |
| `report.json` | 解析結果のすべて |
| `scorecard.json` | セキュリティスコア（0〜100）と、重要度ごとの指摘（`high` / `warning` / `info` / `secure` / `hotspot`） |

スクリプトの最後に、スコアと `high` / `warning` の件数を表示する。

### 指摘の扱い

MobSF の結果では失敗にしていない。配布前に `high` の指摘を確認し、直すか、問題ない理由を記録する。
失敗にしたくなったら、`scripts/mobsf-scan.ps1`（と `scripts/mobsf-scan.sh`）の最後で `high` の件数を見て `exit 1` する。

### バージョンを上げる

結果が勝手に変わらないよう、この文書でコンテナイメージのバージョンを固定している（`v4.5.3`）。
上げるときは、この文書の `docker.io/opensecurity/mobile-security-framework-mobsf:v4.5.3` を書き換え、
[MobSF のリリースノート](https://github.com/MobSF/Mobile-Security-Framework-MobSF/releases)で API の変更がないか確かめる。

## GitHub Actions

今は登録していない。静的解析のワークフロー（`.github/workflows/static-analysis.yml`）は置かず、上の手順で手元で実行する。
あとで CI に載せるときは、手元で全部が通る状態にしてから、Gradle のタスクと MobSF の手順をそのままワークフローに移す
（以前のワークフローは `35444fe` / `3eb73c6` のコミットにある）。

## 道具を足す・バージョンを上げるとき

- バージョンは `gradle/libs.versions.toml` にまとめてある（`detekt`、`jacoco`）。
- detekt を上げたら、`./gradlew detekt` を一度実行し、新しいルールで指摘が増えていないか確かめる。
  増えていたら直すか、`config/detekt/detekt.yml` で扱いを決める。ベースラインに足して黙らせない。
- AGP を上げたら Lint のルールも増える。`./gradlew lintMockDebug` を実行し、同じように扱いを決める。
