# テスター向けビルドの配布

GitHub Actions で APK を作り、Google Workspace の共有ドライブに置いて、Slack に知らせる。
ワークフローは `.github/workflows/tester-build.yml`。

```
手で実行（Actions の画面の Run workflow）
  └▶ APK を作る（選んだ flavor とビルドタイプで。release は Secrets の鍵で署名する）
       ├▶ Actions の成果物に残す（30 日）
       ├▶ 共有ドライブのフォルダにアップロードする（リポジトリ変数 DRIVE_FOLDER_ID があるときだけ）
       └▶ Slack に知らせる（ブランチ、バージョン、コミットと、成功ならドライブのリンク、ドライブに置いていなければ Actions の実行結果へのリンク、
                             失敗なら Actions のログへのリンク）
```

ファイル名は `AndroidSampleApp-<ビルドバリアント>-<バージョン>-<コミット>-<コミットの日時>-r<実行番号>.apk`。

| 部分 | 例 | 出どころ |
| --- | --- | --- |
| ビルドバリアント | `productRelease` | 選んだ flavor とビルドタイプ |
| バージョン | `1.0` | できた APK の中の versionName（`aapt2` で読む。versionNameSuffix まで含むので、アプリの表示と必ず揃う） |
| コミット | `a1b2c3d` | ビルドしたコミットの先頭 7 文字 |
| コミットの日時 | `20261111-1430` | ビルドしたコミットの日時（日本時間） |
| 実行番号 | `r12` | Actions の実行一覧の `#12` |

例: `AndroidSampleApp-productRelease-1.0-a1b2c3d-20261111-1430-r12.apk`

実行番号が入っているので、同じコミットを何度ビルドしても名前は重ならない。
テスターさんから番号を聞けば、どの実行で作ったかがすぐ分かる。

### アプリの中で確かめる

バージョン、コミット番号、コミットの日時は、アプリの `BuildConfig` にも埋めてある。ファイル名と同じ値になる。

| 値 | BuildConfig | アプリ側で読むところ |
| --- | --- | --- |
| バージョン名 | `VERSION_NAME`（最初から入っている） | `AppConfig.buildInfo.versionName` |
| バージョンコード | `VERSION_CODE`（最初から入っている） | `AppConfig.buildInfo.versionCode` |
| コミット番号 | `BUILD_COMMIT`（`app/build.gradle.kts` で足している） | `AppConfig.buildInfo.commit` |
| コミットの日時 | `BUILD_COMMIT_DATE`（同上） | `AppConfig.buildInfo.commitDate` |

CI は `-PbuildCommit` と `-PbuildCommitDate` で値を渡す。手元のビルドでは、ビルドしたときの git の HEAD から読む
（コミットの日時はその PC の時刻帯。git が使えなければ `unknown`）。
画面には `AppConfig.buildInfo.summary`（例: `1.0 (1) / a1b2c3d / 2026-11-11 14:30`）を出せばよい。

## 使い方

### 手で実行する

1. GitHub のリポジトリで **Actions** → 左の一覧の **tester-build** を開く
2. 右の **Run workflow** を押す
3. 次を選んで **Run workflow** で実行する

| 項目 | 選べる値 | 意味 |
| --- | --- | --- |
| Use workflow from | ブランチ | どのブランチのコードでビルドするか |
| flavor | `product` / `mock` | `product` は本番の接続先。`mock` はサーバが無くても動く（通信を `FakeApiInterceptor` が返す） |
| ビルドタイプ | `release` / `debug` | `release` は配布する形。`debug` はデバッグ用 |

選べるのは、`tester-build.yml` が入っているブランチだけ。`develop` から切ったブランチなら入っている。

**Run workflow のボタンは、`tester-build.yml` が既定のブランチ（`develop`）に入っているときだけ表示される。**
GitHub Actions の決まりで、手で実行するワークフロー（`workflow_dispatch`）は既定のブランチにファイルが無いと一覧に出ない。
既定のブランチに入っていれば、ボタンで選んだブランチ側の `tester-build.yml` で実行される。

GitHub CLI からも実行できる。

```sh
gh workflow run tester-build.yml --ref develop -f flavor=product -f build_type=release
```

### 自動では動かない

push では動かない（`on:` は `workflow_dispatch` だけ）。ビルドしたいときに、上の手順で手で実行する。

同じブランチで続けて実行したときは、先の回を止めずに、終わってから次を動かす（`concurrency` の `cancel-in-progress: false`）。
待っている回は 1 つまでで、待っている間にさらに実行すると、待っていた回は取り消されて新しい回が待つ。

push でも動かしたくなったら、`.github/workflows/tester-build.yml` の `on:` に次を足す。

```yaml
on:
  push:
    branches: [develop]
  workflow_dispatch:
    # ...今のまま
```

その場合、push のときは画面の入力が無いので、`env` の `FLAVOR` / `BUILD_TYPE` に既定値を書いておく
（例: `${{ inputs.flavor || 'product' }}`）。

## 最初の準備

一度だけ行う。必要なものは次の 4 つ。

| 準備 | 誰がするか | 作るもの |
| --- | --- | --- |
| 1. 署名の鍵 | 開発者 | GitHub の Secrets 4 つ |
| 2. Google Cloud | Google Cloud のプロジェクトの管理者 | サービスアカウントと Workload Identity 連携。GitHub のリポジトリ変数 2 つ |
| 3. 共有ドライブ | 共有ドライブの管理者 | 置き場所のフォルダ。GitHub のリポジトリ変数 1 つ |
| 4. Slack | Slack のワークスペースの管理者（またはアプリを作れる人） | Incoming Webhook の URL。GitHub の Secrets 1 つ |

### Google Cloud の準備が済むまで

2 と 3 が済むまでは、リポジトリ変数 `DRIVE_FOLDER_ID` を設定しないでおく。
その間は「Google Cloud に認証する」と「共有ドライブにアップロードする」の手順を飛ばし（Actions の画面では「スキップ」と出る）、
APK は Actions の成果物（Artifacts）にだけ置く。Slack には、Actions の実行結果へのリンクを送る。

2 と 3 が済んだら、`GCP_WORKLOAD_IDENTITY_PROVIDER`、`GCP_SERVICE_ACCOUNT`、`DRIVE_FOLDER_ID` の 3 つを設定する。
ワークフローは触らなくてよく、次の実行からドライブへのアップロードが動き出す。
`DRIVE_FOLDER_ID` を最後に設定する（先に設定すると、ほかの 2 つが無いまま認証の手順が動いて失敗する）。

Actions の成果物からダウンロードするには、GitHub にログインしていて、このリポジトリを見る権限が要る。
ダウンロードしたファイルは zip に包まれているので、展開してから端末に入れる。30 日で消える。

### 1. 署名の鍵

release の APK は署名しないと端末に入れられない。テスター配布用の鍵を作り、GitHub の Secrets に入れる。

```sh
keytool -genkeypair -v -keystore tester-release.jks -alias tester \
  -keyalg RSA -keysize 2048 -validity 10000
```

パスワードと名前などを聞かれるので答える。できた鍵を base64 にする。

```sh
base64 -w 0 tester-release.jks > tester-release.jks.base64   # Linux
base64 -i tester-release.jks -o tester-release.jks.base64    # Mac
```

GitHub の **Settings** → **Secrets and variables** → **Actions** → **Secrets** タブで、次を足す。

| Secret | 値 |
| --- | --- |
| `RELEASE_KEYSTORE_BASE64` | `tester-release.jks.base64` の中身 |
| `RELEASE_STORE_PASSWORD` | 鍵ストアのパスワード |
| `RELEASE_KEY_ALIAS` | `keytool` の `-alias` に指定した名前（上の例では `tester`） |
| `RELEASE_KEY_PASSWORD` | 鍵のパスワード。PKCS12 の鍵ストア（JDK 9 以降の `keytool` の既定）では、鍵ストアのパスワードと同じ値 |

**鍵ファイル（`tester-release.jks`）とパスワードは、リポジトリの外の安全な場所に保管する。**
なくすと同じ鍵で署名できなくなり、テスターさんは入っているアプリを消してから入れ直すことになる。
鍵ファイルはリポジトリにコミットしない（`.gitignore` で `*.jks` を外してある）。

ワークフローは、ビルドを始める前に「署名の鍵を確かめる」の手順で、Secrets のパスワードと alias で鍵を開けるかを確かめる。
間違っていれば、ビルドの前に数秒で止まり、どの値が違うかを知らせる（下の「うまく動かないとき」）。

Gradle は、環境変数 `RELEASE_STORE_FILE` があるときだけこの鍵で署名する（`app/build.gradle.kts` の `signingConfigs`）。
手元のビルドや static-analysis の MobSF のジョブでは渡さないので、今までどおり署名なしの APK になる。

debug は、Actions の実行ごとに作られる debug 用の鍵で署名される。鍵が毎回変わるので、
前の debug ビルドが入っている端末に上書きで入れようとすると失敗する。消してから入れ直す。

### 2. Google Cloud（サービスアカウントと Workload Identity 連携）

GitHub Actions は、鍵ファイルを使わずに Google Cloud に認証する（Workload Identity 連携）。
GitHub が実行ごとに発行するトークンを、Google Cloud がサービスアカウントの一時的なトークンに替える仕組みで、
漏れて困る鍵がリポジトリにも Secrets にも残らない。

`gcloud` が使える端末で、次を順に実行する。`PROJECT_ID` は使う Google Cloud のプロジェクトに置き換える。
手順は [google-github-actions/auth の説明](https://github.com/google-github-actions/auth)に沿っている。

```sh
PROJECT_ID=your-project-id
REPO=OmochiKuromitu/AndroidSampleApp

# 使う API を有効にする
gcloud services enable drive.googleapis.com iamcredentials.googleapis.com sts.googleapis.com \
  --project="$PROJECT_ID"

# アップロードに使うサービスアカウント
gcloud iam service-accounts create tester-build-uploader \
  --project="$PROJECT_ID" --display-name="tester-build (GitHub Actions)"

# Workload Identity のプール
gcloud iam workload-identity-pools create github \
  --project="$PROJECT_ID" --location=global --display-name="GitHub Actions"

# プロバイダ。attribute-condition で、このリポジトリ以外からは入れないようにする
gcloud iam workload-identity-pools providers create-oidc android-sample-app \
  --project="$PROJECT_ID" --location=global --workload-identity-pool=github \
  --display-name="AndroidSampleApp" \
  --issuer-uri="https://token.actions.githubusercontent.com" \
  --attribute-mapping="google.subject=assertion.sub,attribute.repository=assertion.repository" \
  --attribute-condition="assertion.repository == '${REPO}'"

# このリポジトリの Actions に、サービスアカウントを使うことを許す
POOL_ID="$(gcloud iam workload-identity-pools describe github \
  --project="$PROJECT_ID" --location=global --format='value(name)')"
gcloud iam service-accounts add-iam-policy-binding \
  "tester-build-uploader@${PROJECT_ID}.iam.gserviceaccount.com" \
  --project="$PROJECT_ID" --role=roles/iam.workloadIdentityUser \
  --member="principalSet://iam.googleapis.com/${POOL_ID}/attribute.repository/${REPO}"

# GitHub に入れる値を表示する
gcloud iam workload-identity-pools providers describe android-sample-app \
  --project="$PROJECT_ID" --location=global --workload-identity-pool=github --format='value(name)'
echo "tester-build-uploader@${PROJECT_ID}.iam.gserviceaccount.com"
```

最後に表示された 2 つを、GitHub の **Variables** タブに足す。どちらも秘密の値ではない
（これだけを知っていても、このリポジトリの Actions 以外からは使えない）。

| 変数 | 値 |
| --- | --- |
| `GCP_WORKLOAD_IDENTITY_PROVIDER` | `projects/123456789/locations/global/workloadIdentityPools/github/providers/android-sample-app` の形 |
| `GCP_SERVICE_ACCOUNT` | `tester-build-uploader@<PROJECT_ID>.iam.gserviceaccount.com` |

### 3. 共有ドライブ

1. 置き場所にする共有ドライブ（の中のフォルダ）を開く
2. 共有ドライブの「メンバーを管理」で、上のサービスアカウント（`tester-build-uploader@...`）を **投稿者** で追加する
   （ファイルを足すだけなので投稿者で足りる。古いファイルを自動で消すなどしたくなったら **コンテンツ管理者** にする）
3. 置き場所のフォルダの URL の、最後の部分がフォルダ ID
   - `https://drive.google.com/drive/folders/1AbCdEfGhIjKlMnOp` なら `1AbCdEfGhIjKlMnOp`
4. GitHub の **Variables** タブに `DRIVE_FOLDER_ID` として足す

マイドライブ（個人のドライブ）のフォルダは使えない。サービスアカウントにはドライブの保存容量がないので、
フォルダを共有しても、アップロードが `storageQuotaExceeded` で失敗する。必ず共有ドライブに置く。

テスターさんには、この共有ドライブ（またはフォルダ）を閲覧できるようにしておく。

### 4. Slack

1. [Slack API の Your Apps](https://api.slack.com/apps) で **Create New App** → **From scratch** でアプリを作る
2. **Incoming Webhooks** を開いて有効にする
3. **Add New Webhook to Workspace** で、知らせるチャンネルを選ぶ
4. 表示された `https://hooks.slack.com/services/...` の URL を、GitHub の **Secrets** タブに `SLACK_WEBHOOK_URL` として足す

この URL を知っていれば誰でもそのチャンネルに投稿できるので、Secrets に入れる（Variables には入れない）。
`SLACK_WEBHOOK_URL` が無いときは、Slack に知らせる手順だけを飛ばす（ビルドとアップロードは行う）。

## GitHub に入れる値の一覧

| 種類 | 名前 | 中身 | 準備 |
| --- | --- | --- | --- |
| Secret | `RELEASE_KEYSTORE_BASE64` | 署名の鍵（base64） | 1 |
| Secret | `RELEASE_STORE_PASSWORD` | 鍵ストアのパスワード | 1 |
| Secret | `RELEASE_KEY_ALIAS` | 鍵の名前 | 1 |
| Secret | `RELEASE_KEY_PASSWORD` | 鍵のパスワード | 1 |
| Variable | `GCP_WORKLOAD_IDENTITY_PROVIDER` | Workload Identity のプロバイダ | 2 |
| Variable | `GCP_SERVICE_ACCOUNT` | サービスアカウントのメールアドレス | 2 |
| Variable | `DRIVE_FOLDER_ID` | 置き場所のフォルダ ID | 3 |
| Secret | `SLACK_WEBHOOK_URL` | Slack の Incoming Webhook の URL | 4 |

## うまく動かないとき

ジョブのログで、どの手順で止まったかを見る。

| 止まった手順 | 出ているもの | 確かめること |
| --- | --- | --- |
| 署名の鍵を用意する | `RELEASE_KEYSTORE_BASE64 が未設定` | Secrets の名前の綴り。base64 にした中身を全部貼ったか |
| 署名の鍵を確かめる | `鍵ストアを開けません` | `RELEASE_STORE_PASSWORD`。または `RELEASE_KEYSTORE_BASE64` の中身が壊れている（Windows の `certutil -encode` で作ると見出し行が付いて壊れる） |
| 署名の鍵を確かめる | `alias が見つかりません` | `RELEASE_KEY_ALIAS`。メッセージに鍵ストアにある alias が出る |
| 署名の鍵を確かめる | `鍵のパスワード（RELEASE_KEY_PASSWORD）が違います` | PKCS12 の鍵ストアなら `RELEASE_STORE_PASSWORD` と同じ値にする。`keytool` は PKCS12 で別の鍵のパスワードを指定されても無視するので、作ったときに入れたつもりの値とは違う |
| 署名の鍵を確かめる | `前後に空白か改行が入っています`（警告） | Secrets を貼るときに空白や改行が一緒に入った。入れ直す |
| APK を作る | `Failed to read key ... from store` | 「署名の鍵を確かめる」が通っているなら、移植先の `build.gradle.kts` で読んでいる環境変数の名前が、ワークフローと揃っているか |
| Google Cloud に認証する | `the given credential is rejected by the attribute condition` | プロバイダの `attribute-condition` のリポジトリ名（大文字・小文字も） |
| Google Cloud に認証する | `Permission 'iam.serviceAccounts.getAccessToken' denied` | `roles/iam.workloadIdentityUser` を付けたか。`principalSet` のリポジトリ名 |
| 共有ドライブにアップロードする | `HTTP 404`（`File not found`） | サービスアカウントを共有ドライブのメンバーに追加したか。`DRIVE_FOLDER_ID` |
| 共有ドライブにアップロードする | `HTTP 403`（`storageQuotaExceeded`） | 共有ドライブではなくマイドライブのフォルダを指定している |
| 共有ドライブにアップロードする | `HTTP 403`（`accessNotConfigured`） | Drive API を有効にしたか（準備 2 の最初のコマンド） |
| Slack に知らせる | `invalid_payload` / `no_service` | `SLACK_WEBHOOK_URL` の URL。Webhook を消していないか |

ドライブに置けなかったときも、APK は Actions の実行結果の **Artifacts** に残っている（30 日）。

## ワークフローのファイルを編集するとき

ローカルで編集して push しても、GitHub の画面（ファイルを開いて鉛筆のボタン）で編集してコミットしてもよい。

- GitHub の画面で編集したあとは、ローカルで `git pull` してから作業を続ける（そのまま push すると衝突する）。
- 編集したファイルが使われるのは、そのブランチでの実行から。手で実行するときに選んだブランチのファイルが使われる。
