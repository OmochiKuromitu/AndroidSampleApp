# AndroidSampleApp

Jetpack Compose + MVI + Hilt で書いた Android アプリのサンプル。
MVI の設計（第 1 部）は題材に依存しない形にしてあり、そのまま他のアプリに持ち込める。
題材には「壁に掛けて使う操作パネル」を選んでいる（第 2 部）。
エアコンの操作、呼び出しへの応答、通知一覧、無操作でのスリープを持ち、
通信はすべて HTTP（Retrofit）で行う。

- 下部バーで 4 タブ（トップ / エアコン / 連絡先 / スリープ）
- 画面遷移も含めて Intent → Reducer を通す
- flavor は `mock`（サーバ不要）と `product`（サーバに接続）

このドキュメントは 2 部に分かれている。

- **第 1 部 MVI の設計** — レイヤ、MVI の流れ、ファイル構成、遷移、状態の置き場所。題材に依存しない。
- **第 2 部 サンプルの題材と仕様** — 操作パネルとしての振る舞い（機器の状態、スリープ、通知など）。
  第 1 部の規約をどう当てはめたかの実例でもある。

## ビルド

壁付けパネル想定のため **縦固定**（`android:screenOrientation="portrait"`）。
プレビューの寸法も縦長にしてある。

Android Studio でこのディレクトリを開く。実行構成は 4 つ（mock/product × debug/release）。
サーバが無いときは **mockDebug** を選ぶ。擬似 API（`FakeApiInterceptor`）が
エアコンの状態・着信・通知を返す。product の接続先（`https://api.example.com`）と
端末の ID はサンプル用の値なので、使う環境に合わせて `app/build.gradle.kts` で差し替える。

- compileSdk / targetSdk 35, minSdk 24
- AGP 8.7.3 / Kotlin 2.0.21 / Gradle 8.11.1 / Hilt 2.52 (KSP)
- Compose BOM 2024.12.01, Navigation Compose 2.8.5
- Retrofit 2.11.0 / OkHttp 4.12.0 / kotlinx.serialization

## ライセンス

[MIT No Attribution (MIT-0)](LICENSE)。著作権表示を残さずに複製・改変・再配布してよい。

---

# 第 1 部 MVI の設計

## レイヤと依存の向き

```
        ui  ──────▶  domain  ◀──────  data
        │            (interface)        │
        │                               ├─▶ network   (HTTP: Retrofit / OkHttp)
        └─▶ core (共有状態の器, MVI の土台)
                     ▲                  │
                     └──────────────────┘
                       共有状態への書き込みは data と、状態の持ち主（IdleTimer・各マネージャー）だけ

        di      ─▶ interface と実装の結線
        config  ─▶ flavor 由来の設定（BuildConfig の読み口）
```

矢印は依存の向き。**ui は data / network / model を知らない**。domain の型、interface、UseCase だけを見る。
実装の差し替えは `di/RepositoryModule` の `@Binds` で完結する。

## MVI のデータの流れ

```
     ┌──────────────── State ────────────────┐
     │                                        │
     ▼                                        │
 Composable ──onIntent(Intent)──▶ ViewModel ──┤
     ▲                               │        │
     │                               ├── Reducer(state, intent) -> state   純粋関数
     └────collect(Effect)────────────┘        │
                             handle(...) ─────┘  UseCase 呼び出し・Effect 送出・追加 onIntent
```

- **State** — 画面が描画に使う唯一の入力。不変の data class。
- **Intent** — 状態を変えうる入力すべて。ユーザー操作に限らず、
  購読している値の変化（`AirconChanged`）や通信の結果（`CommandSucceeded`）も Intent にして戻す。
  こうすると状態が変わる経路が Reducer 1 か所に収まる。
- **Reducer** — `(State, Intent) -> State`。I/O・時刻取得・コルーチン起動を書かない。
- **Effect** — 状態として持つべきでない一回きりの出来事（遷移、スナックバー）。

State と Effect を分けるのが要点。遷移を State に持たせると、画面回転などの再生成のたびに
同じ遷移が走る。逆に「選択中のタブ」を Effect にすると、復帰したときに復元できない。

## ui/<feature> の 7 ファイル

画面 1 つにつき、必ずこの 7 つを置く。ファイル名で役割が分かる状態を保つ。

| ファイル | 中身 |
| --- | --- |
| `XxxState.kt` | 画面の状態。`UiState` を実装した data class |
| `XxxIntent.kt` | 入力の一覧。`UiIntent` を実装した sealed interface |
| `XxxEffect.kt` | 一回きりの出来事。`UiEffect` を実装した sealed interface。遷移の命令は書かない |
| `XxxReducer.kt` | `(State, Intent) -> State` の純粋関数 |
| `XxxViewModel.kt` | `ViewModel` を継承し、`_uiState` と `_effect` を自分で持つ。`handle()` に副作用を隔離 |
| `XxxRoute.kt` | 配線。ViewModel の取得、State の購読、Effect の受け取り、`LaunchedEffect` |
| `XxxScreen.kt` | 表示。State を描き、操作をコールバックで返すだけ。Intent は知らない |

**Route と Screen を分ける。**

- `XxxRoute` — `AppNavigation` から呼ばれる入口。`hiltViewModel()` で ViewModel を取り、
  State を購読し、Effect を受けて呼び出し元のコールバックへ流す。`LaunchedEffect` もここ。
  Screen から返ってきた操作を Intent に変えて `onIntent` に渡すのもここだけ。
- `XxxScreen` — `state` と、操作ごとのコールバック（`onAnswerClick: () -> Unit`、
  `onModeSelect: (AirconMode) -> Unit` など）だけを受け取る。
  ViewModel も Intent も Effect も知らない。

Screen を表示だけに保つのが目的。副作用の配線が混ざると、見た目を直すつもりで
ライフサイクルの都合を読む羽目になる。プレビューの都合もある。`hiltViewModel()` は
プレビューで解決できないので、State を引数で渡せる `XxxScreen` に `@PanelPreview` を
付けて、State を差し替えながら見た目を確認する。

```kotlin
@PanelPreview
@Composable
private fun AirconScreenOfflinePreview() {
    PreviewSurface {
        AirconScreen(
            state = AirconState(connectionState = ConnectionState.DISCONNECTED),
            onPowerToggle = {},
            onTemperatureDownClick = {},
            onTemperatureUpClick = {},
            onModeSelect = {},
        )
    }
}
```

`PanelPreview` と `PreviewSurface` は `ui/common/Previews.kt` にある。
前者は壁付けパネル想定の横長サイズでライトとダークを並べる複合アノテーション、
後者はテーマと背景色を実機と揃えるための下敷き。`AppTheme` を通さないと
`Dimensions` とタイポグラフィが既定値になり、実機と違う見た目のまま調整してしまう。

現在は 4 画面に 10 個のプレビューがある（着信あり / 待機中、運転中 / 停止中 / 未接続、
スリープ / スワイプ中、接続あり / 切断）。それぞれライトとダークで描かれる。

土台は `core/mvi/Mvi.kt`（`UiState` / `UiIntent` / `UiEffect` / `Reducer`）だけ。ViewModel の基底クラスは置かず、
各 ViewModel が同じ形を自分で書く。どの ViewModel を開いても、状態と Effect の持ち方が
その場で読めるようにするため。

```kotlin
@HiltViewModel
class XxxViewModel @Inject constructor(/* UseCase */) : ViewModel() {
    private val _uiState = MutableStateFlow(XxxState())
    val uiState: StateFlow<XxxState> = _uiState.asStateFlow()

    private val _effect = Channel<XxxEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private val reducer = XxxReducer()

    init { /* 購読して onIntent に流す */ }

    fun onIntent(intent: XxxIntent) {
        // Reducer で次の状態を作って入れる。読んでから書くまでに別の更新が入ったらやり直す。
        // そのあと handle(intent, previous, current) を並行に起動する。
    }
}
```

- **プロパティは `init` より上に書く。** Kotlin は上から順に初期化するので、`init` の中で
  `onIntent` を呼んだときに `_uiState` や `reducer` がまだ無いと落ちる。
- 副作用（`handle`）は並行に走らせる。長い I/O が、後から来た Intent の反映を止めないようにするため。

Route は Effect を `LaunchedEffect(viewModel) { viewModel.effect.collect { ... } }` で受け取る。

- 画面が裏に回っている間も受け取る。前面に戻るまで溜めることはしない
  （壁付けでほぼ常に前面にいる前提。裏にいる間に出たスナックバーは、誰も見ないまま消えることがある）。
- Effect は Channel なので、1 つの Effect は 1 回しか届かない。1 画面で `collect` するのは 1 か所だけにする。
- 親から受け取ったコールバックを Effect で呼ぶときは `rememberUpdatedState` 越しに呼ぶ
  （`SleepRoute` が見本）。`LaunchedEffect` の中身は最初に起動したときのまま動くため。

## 画面遷移の扱い

**遷移は `ui/navigation/AppNavigation` だけが行う。** `NavController` を持つのもここだけ。

各画面の ViewModel は「何が起きたか」を Effect で返すだけで、どこへ行くかは決めない。
画面の Composable も `navController` を触らない。判断と実行が散っていると、
「この画面はどこから来てどこへ行くのか」を追うのにファイルを行き来することになる。

| 出来事 | 誰が伝えるか | AppNavigation がすること |
| --- | --- | --- |
| タブがタップされた | `MainRoute` の `onTabClick` | そのタブへ navigate |
| スリープタブがタップされた | 同上 | `IdleTimer.onSleepRequested()`。遷移は下の行で起きる |
| スリープ状態になった / 解けた | `IdleTimer.isSleeping` | スリープ画面へ navigate / 元のタブへ戻る |
| 解除の操作が成立した | `SleepEffect.Unlocked` | `IdleTimer.wake()` |
| 通知が選ばれた | `SleepEffect.NoticeSelected(destination)` | その飛び先へ navigate して `wake()` |
| お知らせタブの通知が選ばれた | `ContactEffect.NoticeSelected(destination)` | その飛び先へ navigate（起きているので `wake()` は不要） |
| 着信した | `IncomingCallRouter` | トップへ navigate して `wake()` |

Effect の名前が `NavigateToXxx` ではなく `Unlocked` / `NoticeSelected` なのは意図的。
ViewModel は出来事を報告するだけで、命令はしない。

NavHost は 1 つで、`top` / `aircon` / `contact` / `sleep` を持つ。呼ぶのは各画面の `XxxRoute`。
行き先は `Route` の定数だけで指定し、どのタブを選択状態にするかは
`MainTab.fromRoute()` がそこから引く。ルートとタブを別々に書くとずれるため。
`ui/main/MainRoute` はヘッダーと下部バーの枠で、中身はスロットで受け取る。
スリープ画面だけこの枠を被せずに出す。

選択中のタブは State に持たない。どのタブを表示しているかは NavController の現在地であって
画面の状態ではないので、`AppNavigation` が決めて `MainRoute` に引数で渡す。

Composable には `@Inject` できないので、必要なものは ViewModel を窓口にして取る。
Activity に持たせて引数で降ろす手もあるが、遷移に必要なものが増えるたびに
`MainActivity` が太るため、`AppNavigation` 側で閉じている。

**窓口は責務ごとに分ける。**

| 窓口 | 中身 | 使う場所 |
| --- | --- | --- |
| `IdleTimerViewModel` | `IdleTimer`（スリープ状態と操作） | `AppNavigation` |
| `IncomingCallViewModel` | 着信の `StateFlow` | `IncomingCallRouter` |
| `MissedCallViewModel` | `MissedCallManager` の取得の促し（不在着信の件数と通知一覧） | `AppNavigation` |

「`AppNavigation` が必要とするもの」という 1 つの入れ物にまとめない。それは責務ではなく、
基準が無い入れ物は画面が増えるたびに無関係なものが同居して太る。
1 つの窓口が変わる理由は 1 つに保つ。

いずれも画面ではないので MVI は敷かず、状態を持たない素通しにしてある。
状態の持ち主は `IdleTimer`、`AppStateHolder`、各マネージャー。

なお `App`（Application）は `IdleTimer` を直接注入している。こちらは
`hiltViewModel()` が使えないため。`IdleTimer` が ViewModel ではなく `@Singleton`
である理由もここにある。

## 画面に値を渡す

`contact` だけルートに引数を持つ。スリープ画面の通知から「不在着信があった」で飛んだときに、
電話帳ではなく履歴から開くため。

```
通知（destination = CONTACT_MISSED）
  └▶ NoticeDestination.Contact(hasMissedCall = true)     ← ドメインは事実だけを持つ
      └▶ toRoute() ─▶ "contact?list=history"             ← ui 層が「だから履歴」と決める
          └▶ ContactViewModel が SavedStateHandle から受け取り、初期状態のタブにする
```

飛び先の型を `enum` から `sealed interface` にしたのは、飛び先によって
伴う情報が違うため。`Contact` だけが不在フラグを持つ。

引数は初期状態として `initialState` で解決する。Intent にすると、取得が終わる前に
一瞬だけ電話帳が見えてから履歴へ切り替わる。

引数つきのルートへ飛ぶときは `restoreState` を使わない。復元すると保存済みのエントリが
そのまま戻り、新しく渡した引数が無視されるため（`navigateToRoute(route, restore = false)`）。

## 状態をどこに置くか

判断の目安は「全画面が見るか」ではなく **書き手と読み手の数**。

| 置き場所 | 何を置くか | 書き手 |
| --- | --- | --- |
| `XxxState` | 画面固有の状態（送信中フラグ、入力中の値、表示の進み具合） | その画面の Reducer |
| 共有状態の器（このサンプルでは `core/AppStateHolder`） | 複数の書き手が更新し、複数の画面が読む状態 | data 層のリポジトリだけ |
| 専用の `@Singleton` | 書き手は 1 つだが、画面をまたいで共有するもの | そのクラス自身 |

読み手が 1 画面なら共有の器を作らず、その画面の `XxxState` に持たせる。
読み手が 2 か所以上になった時点で `@Singleton` に引き上げる。
共有の器は書き手が複数いて初めて元が取れる。書き手が 1 つなら、そのクラスに持たせる。
このサンプルでの当てはめ方は第 2 部の「共有状態 — AppStateHolder」と「スリープに入る条件」にある。

## HTTP で取るデータ — リポジトリの StateFlow

電話帳・履歴（`ContactRepository.addressBook`）と API の通知（`NoticeRepository.notices`）は、
取った結果をリポジトリが `MutableStateFlow` で持ち、`StateFlow` で公開する。
ViewModel は `init` で購読し、値が流れるたびに Intent にして Reducer に通す。
定期的に取る機器の状態（`AppStateHolder`）と同じ受け取り方に揃えてある。

```
ViewModel.init
  └─ ObserveXxxUseCase().filterNotNull().collect { dispatch(XxxChanged(it)) }

ViewModel.handle(Started)
  └─ RefreshXxxUseCase() ─▶ API ─▶ リポジトリの MutableStateFlow に入る ─▶ 上の collect に流れる
       失敗したときだけ dispatch(LoadFailed)
```

- **まだ一度も取れていない間は `null`。** 「まだ取っていない空」と「取ったら空だった」を分けるため。
  ViewModel は `null` を捨て、State は「一度でも受け取れたか」のフラグを持つ。
- **読み込み中は、そのフラグから決める**（`isLoading = !受け取り済み && !失敗`）。
  「取り直しが終わったら読み込み中を解く」を Intent で待つと、`StateFlow` は同じ値を
  入れ直しても流れないので、取り直した結果が前と同じだったときに止まる。
- **リポジトリは `@Singleton` なので前回の値が残る。** 画面を開き直すと、取り直しを待たずに
  前回の一覧がまず出て、取り直しが終われば差し替わる（スリープ画面は入るたびに ViewModel が
  作り直されるが、通知はすぐ出る）。取り直しに失敗しても、受け取り済みの一覧は出したままにする。
- 電話帳と履歴の取り直しのきっかけは、画面を開いたとき。API の通知は読み手が 2 画面あるので
  ViewModel が直接は購読せず、`MissedCallManager` が購読して並べたものを見せる（第 2 部の「通知一覧 — MissedCallManager」）。
  定期的な取り直しはしていない。

## ディレクトリ

```
app/src/main/java/com/example/androidsampleapp/
├── App.kt                  @HiltAndroidApp
├── MainActivity.kt
├── config/                 flavor に対応した設定（AppConfig）
├── core/                   AppStateHolder（機器の状態）/ MissedCallManager（不在着信の件数と通知一覧）
│   └── mvi/                UiState / UiIntent / UiEffect / Reducer
├── di/                     Hilt モジュール（AppModule / NetworkModule / RepositoryModule / Qualifiers）
├── domain/
│   ├── model/              Aircon / AirconSpec / ConnectionState / IncomingCall / Notice
│   ├── repository/         DeviceRepository / AirconRepository / NoticeRepository / ContactRepository（interface）
│   └── usecase/            ObserveXxx / SetXxx / AnswerCall …
├── data/                   Repository 実装
├── network/                DeviceApi / NoticeApi / ContactApi（Retrofit の interface）/ FakeApiInterceptor
├── model/                  API の本文とレスポンス（DeviceRequest / DeviceStatusResponse / NoticeResponse …）
└── ui/
    ├── navigation/         AppNavigation / IncomingCallRouter / IdleTimer
    │                       + 責務ごとの窓口 ViewModel（IdleTimer / IncomingCall）
    ├── common/             Route / AppHeader / NoticeList / 共通コンポーネント / プレビュー定義
    ├── theme/              Color / Type / Dimensions / Theme
    ├── main/               ヘッダーと BottomNaviBar の枠（MVI 7 ファイル + BottomNaviBar）
    ├── top/                メイン画面に入れる画面（MVI 7 ファイル）
    ├── aircon/             エアコン操作（MVI 7 ファイル）
    ├── contact/            連絡先。画面内で電話帳と履歴を出し分ける（MVI 7 ファイル）
    └── sleep/              スリープ画面（MVI 7 ファイル）
```

`model/` と `domain/model/` の使い分け:

- `model/` — 通信の語彙。API に送る本文と、受け取るレスポンス。
- `domain/model/` — アプリが扱う語彙。画面と UseCase はこちらだけを見る。
  設定温度の範囲と刻み（`AirconSpec`）のように、画面と UseCase の両方が使う値もここに置く。

変換は data 層（各 `XxxRepositoryImpl` と `data/CodeMapping`）が担う。
API の形が変わっても影響を network・model・data に閉じる。

### 足すとき

手順と雛形は `.claude/skills/android-screen/SKILL.md` にまとめてある。
Claude Code はこのリポジトリで作業するとき自動で参照する。
このドキュメントが「なぜそうなっているか」、スキルが「何をどこへ書くか」を持つ。

- **画面を 1 つ足す** — `ui/<name>/` に 7 ファイル。ViewModel は `@HiltViewModel`。
  遷移が要るなら Effect で「何が起きたか」を返し、行き先は `AppNavigation` に書く。
- **タブを 1 つ足す** — `Route` に 1 行、`MainTab` に 1 行、`AppNavigation` の NavHost に
  `composable` を 1 つ。選択するタブは `MainTab.fromRoute(Route.XXX)` で引く。
- **API を 1 つ足す** — `model/` に本文とレスポンスの型、`network/` の Retrofit の interface に 1 本、
  `FakeApiInterceptor` に応答、`domain/repository` に口、`data` に実装、`domain/usecase` に UseCase。

## テスト

Reducer は Android に依存しない純粋な処理なので、JVM テストで完結する。

```
./gradlew testMockDebugUnitTest
```

`app/src/test/` には各画面の Reducer のほか、リポジトリ（MockWebServer か偽の API を相手にする）、
マネージャー、`IdleTimer`、`FakeApiInterceptor` のテストがある。
時間に依存するものは `kotlinx-coroutines-test` の `runTest` と仮想時間で書く。

---

# 第 2 部 サンプルの題材と仕様

ここからは操作パネルという題材に固有の振る舞い。第 1 部の規約を実際の機能に当てはめた例として読める。

画面は 4 つ。

| 画面 | 中身 |
| --- | --- |
| トップ | 着信のカード（応答 / 拒否）とエアコンの現在値 |
| エアコン | 運転の入り切り、設定温度の上げ下げ、運転モードの選択 |
| 連絡先 | 電話帳 / 履歴 / お知らせの 3 タブ |
| スリープ | 時刻と通知一覧。下端からのスワイプで解除 |

## 共有状態 — AppStateHolder

接続状態・着信・エアコンの現在値は、`core/AppStateHolder` が単独で持つ。

- **書き込むのは data 層だけ** — 状態取得の結果を反映する `data/DeviceRepositoryImpl` と、
  エアコンの操作 API の応答を反映する `data/AirconRepositoryImpl`。
- 画面と ViewModel は読むだけ。UseCase 経由で `StateFlow` を受け取り、
  変化を Intent に変換して自分の Reducer に流す。
- 画面固有の状態（送信中フラグなど）はここに置かず、各 `XxxState` が持つ。

ここに置くのは **機器の状態**（状態取得 API で定期的に取るもの）に限る。「全画面が見る値だから」で
何でも入れると、書き手が増えて所在が追えなくなる。判断の目安は書き手と読み手の数。

スリープ中かどうかがその例で、これは `AppStateHolder` ではなく `IdleTimer` が自分で持つ。
書き手は `IdleTimer` だけ、読み手も `AppNavigation` だけなので、共有の器を通す理由がない。
通すと、状態を持つ場所と更新を決める場所が分かれてしまい、
`IdleTimer` を読むだけではスリープの挙動が追えなくなる。

## 状態の定期取得

HTTP は黙っていても届かないので、機器の状態は一定間隔で取りに行く。

```
App（ProcessLifecycleOwner が STARTED の間だけ）
  └─ DeviceRepository.monitor()              AppConfig.pollInterval（既定 3 秒）ごとに繰り返す
       └─ DeviceApi.getStatus()              POST {apiBaseUrl}/device/status
            └─ AppStateHolder                接続状態・着信・エアコンの現在値を更新

操作: ViewModel -> UseCase -> Repository -> DeviceApi（POST aircon/power など）
      操作 API は操作後の現在値を返すので、次の定期取得を待たずに AppStateHolder に反映する
```

- **接続状態は「直近の取得が成功したか」で決める。** 失敗したら未接続にし、次の周期で取り直す。
  一度の失敗では止めない。
- **前面にいる間だけ取る。** `repeatOnLifecycle(STARTED)` で、裏に回ると止まり、戻ると再開する。
  画面が無い間に取り続けても見る人がいないので、常駐のサービスは置いていない。
- 着信に気づくまでの遅れは `pollInterval` で決まる。短くするほど通信が増える。

API はどれも POST で、本文で端末の ID（`AppConfig.deviceId`）を名乗る。

| API | 本文 | 応答 |
| --- | --- | --- |
| `device/status` | `deviceId` | エアコンの現在値と着信（無ければ `null`） |
| `aircon/power` / `aircon/mode` / `aircon/temperature` | `deviceId` と変える値 | 操作後のエアコンの現在値 |
| `calls/answer` / `calls/reject` | `deviceId` と `roomId` | なし（204） |
| `notices/list` / `notices/delete` | `deviceId` | 通知の配列 / なし（204） |
| `contacts/list` / `calls/history` | `deviceId` | 電話帳の配列 / 通話履歴の配列（新しい順） |
| `missed-calls/count` / `missed-calls/read` | `deviceId` | 未確認の不在着信の件数（`{"count":n}`） / なし（204） |

パスと JSON の形はサンプル用の仮の仕様。mock flavor では `FakeApiInterceptor` がこれらに応答し、
エアコンの現在値、着信、通話履歴と未確認の不在着信の件数、通知を消去したかどうかを覚えている。

着信は最初の状態取得から 8 秒後に鳴る。20 秒のうちに応答か拒否が無ければ不在着信になり、
件数が 1 つ増えて履歴にも残る（次のタブ移動で下部バーのバッジに出る）。
どちらで終わっても 3 分後にまた鳴る。着信のたびにトップ画面へ移ってスリープも解けるので、
間隔は短くしすぎない（`FakeApiInterceptor` の `CALL_INTERVAL_MS`）。

## スリープに入る条件

状態と判断は `ui/navigation/IdleTimer` が一手に持ち、`isSleeping` を立てるだけ。
遷移は、それを見た `AppNavigation` が行う。きっかけは 3 つ。

| きっかけ | 呼ぶもの | 挙動 |
| --- | --- | --- |
| 無操作が 30 秒続いた | 内部タイマー（`AppConfig.sleepTimeout`） | どの画面からでもスリープ |
| バックグラウンドに移った（他アプリへの移動、画面の消灯） | `IdleTimer.onEnteredBackground()` | 無操作時間に関係なく即スリープ |
| スリープタブが選ばれた | `IdleTimer.onSleepRequested()` | 即スリープ |

操作の検知は `AppNavigation` のルートに置いた `pointerInput` が担う。
`PointerEventPass.Initial` で子より先に覗くだけなので、画面側の操作は妨げない。

**タイマーと状態は分けてある。**

| 呼ぶもの | タイマー | `isSleeping` |
| --- | --- | --- |
| `resetTimer()` | 測り直す（スリープ中は何もしない） | 変えない |
| `pauseTimer()` | 止める | 変えない |
| `resumeTimer()` | 測り直しから再開 | 変えない |
| `wake()` | 測り直す | `false` にする |
| `onEnteredBackground()` など | 止める | `true` にする |

混ぜると「タイマーを再開したら勝手に起きた」のような挙動になる。
触れただけで解除されないのは `resetTimer()` が `isSleeping` を変えないから。
解除は `wake()` だけが行い、解除ジェスチャと着信からしか呼ばれない。

バックグラウンドへの移行は `App` が `ProcessLifecycleOwner` の `ON_STOP` で拾う。
他アプリへの移動も、画面の消灯もここに来る。
Activity の `onStop` を使わないのは、構成変更（画面サイズ、ロケール、
ダークテーマの切り替えなど）による再生成でも呼ばれてしまうから。
`ProcessLifecycleOwner` は構成変更を除外する。

復帰したときではなく離れたときに倒しているのは、復帰時に判定すると遷移が走るまでの
1 フレームだけ前の画面が見えることがあるため。離れる時点で倒しておけば、
戻ってきた最初の描画がスリープ画面になる。

## スリープの解除

画面下端の帯を上にスワイプしたときだけ解除する。タップでは解除しない。
拭き掃除や誤接触で操作画面に戻らないようにするため。

| 項目 | 既定値 | 置き場所 |
| --- | --- | --- |
| 受け付ける帯の高さ | 50dp | `Dimensions.unlockAreaHeight` |
| 解除に必要な移動量 | 120dp | `Dimensions.unlockDistance` |
| ヒントのバーの大きさ | 108×4dp | `Dimensions.unlockHintWidth` / `unlockHintHeight` |

帯より移動量が大きいのは矛盾ではない。ドラッグは始まった位置で受け付けが決まり、
その後は帯の外へ出ても追跡が続く。帯は「どこから始めたら解除操作とみなすか」だけを決める。

指の移動量は画面側で 0f..1f に正規化し、`SleepIntent.UnlockDragged` として
Reducer に渡す。`SleepState.unlockProgress` がそれを保持し、ヒント表示が
その値に応じて持ち上がって濃くなる。押し戻せば進み具合も戻るので、途中でやめられる。

解除は `SleepViewModel.handle()` が `previous` と `current` を見比べ、
進み具合が 1.0 に達した瞬間の 1 回だけ `SleepEffect.Wake` を出す。
指がさらに動いても重ねて送らない。

## 通知一覧 — MissedCallManager

通知はスリープ画面（時刻表示の下）と、連絡先画面の「お知らせ」タブの 2 か所に出す。
どちらも同じ一覧で、消去もどちらから押しても同じものが消える。
消去ボタンは一覧の右上に小さく置く。お知らせタブのバッジは一覧の件数。

消去は押した時点では消さず、「通知をすべて消しますか？」の確認ダイアログを 1 度出す。
消したものは戻せないので、押し間違いをここで止める。ダイアログは `ui/common/NoticeList` の
`NoticeClearConfirmDialog` を両画面で使い、出しているかどうかは各画面の State
（`SleepState.isClearConfirmVisible` / `ContactState.isClearConfirmVisible`）が持つ。

1 件ずつ白いカードで出し、1 行目に「種別・タイトル・時刻」、2 行目に詳細を置く。
**警報（`ALERT`）は上にまとめ、それ以外との間に線を引く。** 並べ替えは表示の都合なので
`ui/common/NoticeList` で行い、グループの中はサーバから来た順（新しいものが先頭）のまま。

読み手が 2 画面あるので、一覧は `core/MissedCallManager`（`@Singleton`）が不在着信の件数と一緒に持つ。
取得のきっかけは `AppNavigation` が決め、マネージャーは呼ばれたら取るだけ。
画面の ViewModel は `noticeSnapshot` を購読するだけで、自分では取りに行かない。

件数と一覧を 1 つのマネージャーに置いているのは、どちらも HTTP で取る共有の状態で、
同じ節目（起動直後 / タブ移動 / スリープ画面が前に出た）に取り直すため。
別々にしていたときは、`AppNavigation` が 3 か所すべてで 2 つの `refresh()` を並べて呼んでいた。
今は `refresh()` 1 つで両方を取る。ただし実行中の要求は種類ごとに持つ
（1 つにすると、通知の消去が件数の取得を打ち切る、といった無関係な取りやめが起きる）。

```
AppNavigation（タブ移動 / スリープ画面が前に出た / 起動直後）
  └▶ MissedCallManager.refresh() ─▶ NoticeUseCase.refresh() ─▶ POST /notices/list
                                  └▶ NoticeRepository.notices
                                      └▶ noticeSnapshot: StateFlow<NoticeSnapshot>（新しい順の一覧 / 読み込み中 / 失敗）
                                          ├▶ SleepViewModel   → NoticesChanged
                                          └▶ ContactViewModel → NoticesChanged
```

一覧の持ち主は `NoticeRepository` で、`MissedCallManager` は `NoticeUseCase` から購読して失敗の有無と合わせるだけ。
`Notice.occurredAt` は表示用の文字列ではなく epoch ミリ秒で持ち、
整形は `NoticeList` で行う（minSdk 24 なので `java.time` は使わない）。

API は取得と消去の 2 本。`MissedCallManager` は `NoticeUseCase` を通して呼び、リポジトリは直接触らない。

**UseCase は関心事ごとに 1 クラス。** 通知なら `NoticeUseCase` 1 つに、見る・取り直す・消すをまとめる。
不在着信は `MissedCallUseCase`（件数を取る・既読にする）。
どの API をどの順で呼ぶか（消去 → 取り直し、既読 → 件数の取り直し）は UseCase が決め、
いつ呼ぶか、実行中の要求を間引くか打ち切るか、失敗をどう見せるかはマネージャーが決める。

```
XxxIntent.ClearNoticesClicked   ─▶ 確認ダイアログを出すだけ（まだ消さない）
XxxIntent.ClearNoticesConfirmed ─▶ MissedCallManager.clearNotices() ─▶ NoticeUseCase.clear() ─▶ POST /notices/delete
                                                                                            └▶ POST /notices/list（取り直し）
                  結果は NoticeRepository.notices に入り、noticeSnapshot を通って NoticesChanged として流れる
                  失敗も noticeSnapshot の loadFailed として同じ経路で流れる
```

消去が取り直しまで行うのは、消している間に届いた通知を落とさないため。
呼び出し側は、API が 2 本であることを知らずに済む。
取得も消去も同じ `noticeSnapshot` に戻るので、画面の経路は 1 本のままになる。

読み込み中は `NoticeSnapshot.isLoading`（通知を一度も受け取れておらず、失敗もしていない間）で、
第 1 部の「HTTP で取るデータ」と同じく「受け取れたか」から決める。

`refresh()` は実行中の要求があれば何もしない（重なって遅い順に上書きされるのを防ぐ）。
失敗しても前回の一覧を残し、失敗だけを立てる。明示的な再試行ボタンは置いていない
（次に画面が切り替われば取り直す）。`clearNotices()` は利用者の操作なので取りやめず、
実行中の取得があれば打ち切る。

**通信は Retrofit + OkHttp、JSON は kotlinx.serialization。** `network/NoticeApi` が
`POST {apiBaseUrl}/notices/list`（取得）と `POST {apiBaseUrl}/notices/delete`（消去）を叩き、
`NoticeRepositoryImpl` が `toDomain()` で変換する。組み立ては `di/NetworkModule`。
エンドポイントは `AppConfig.apiBaseUrl`（flavor ごとに `buildConfigField` で設定、末尾の `/` は付けない）で、
パスとレスポンスの形はサンプル用の仮の仕様。

**どちらの API も本文で端末の ID を名乗る**（`model/DeviceRequest`、`{"deviceId":"..."}`）。
メソッドが同じ POST なので、取得と消去はパスで分けている。ID を詰めるのは `NoticeRepositoryImpl` で、
値は `AppConfig.deviceId`（固定の設定値なので、接続先と同じく flavor ごとの `buildConfigField` で持つ）。
ID が要るのは通信の都合なので data 層で閉じ、`NoticeRepository` の口は変えていない。
`MissedCallManager` や画面は ID を知らない。

**サーバが無くても動く。** mock flavor（`AppConfig.useFakeApi`）では `network/FakeApiInterceptor` を
OkHttp に挟み、通信の手前で JSON を返す。Retrofit の呼び出しと JSON の解釈、2xx 以外で
例外になるところまでは本物と同じ経路を通る。消去したかどうかもこの Interceptor が覚えている。
本文に `deviceId` が無ければ 400 を返すので、ID の付け忘れには mock の段階で気づける。
本物のサーバを使うなら、フラグを落とすだけで本物を叩く。リポジトリから上は変わらない。

**飛び先は通知自身が持つ。**

```kotlin
data class Notice(
    val id: String,
    val category: NoticeCategory,   // CALL / AIRCON / ALERT / INFO。一覧ではタグとして色分け
    val title: String?,             // 種別の横に出す見出し。無い通知もある
    val message: String,            // 見出しの下に出す詳細
    val occurredAt: Long,           // epoch ミリ秒。整形は ui 層
    val destination: NoticeDestination,   // TOP / AIRCON
)
```

一覧側に「この分類ならここ」という対応表を持たせない。飛び先を増やすときは
`NoticeDestination` と、そのタブへの対応（`toMainTab()`）だけを触ればよい。
ドメインはルート文字列を知らず、変換は `ui/main` に置いてある。

タップしたときの経路はこうなる。

```
NoticeClicked ─▶ Reducer（状態は変えない）
              └▶ handle() ─▶ SleepEffect.NoticeSelected(destination)
                              └▶ AppNavigation がその飛び先へ navigate して wake()
```

なお通知のタップは、下端スワイプを経ずに解除される唯一の経路になる。
意図した操作なので許しているが、誤接触も通してしまう点は承知のうえ。

## 不在着信のバッジ — MissedCallManager

不在着信の件数は下部バーの連絡先タブと、連絡先画面の履歴タブの 2 か所が見る。
読み手が複数いるので、`core/MissedCallManager` が保持する。

```
AppNavigation（タブ移動 / スリープ画面が前に出た / 起動直後）
  └▶ MissedCallManager.refresh() ─▶ MissedCallUseCase.getCount() ─▶ POST /missed-calls/count
                                     └▶ missedCallCount: StateFlow
                                         ├▶ MainViewModel   → 下部バーのバッジ
                                         └▶ ContactViewModel → 履歴タブのバッジ

ContactViewModel（履歴タブを見せた）
  └▶ MissedCallManager.markAsRead() ─▶ MissedCallUseCase.markAsRead() ─▶ POST /missed-calls/read
                                                                        └▶ POST /missed-calls/count（取り直し）
                                                                            └▶ 同じ StateFlow に戻る
```

既読は履歴タブを見せた時点で呼ぶ。連絡先画面を履歴から開いたとき（通知経由）と、
画面内で履歴タブに切り替えたときの 2 か所。件数で間引かないのは、件数がまだ届いていない
タイミングで開かれると取りこぼすため。既読 API は何度呼んでも同じ結果になる前提。

既読のあとに取り直すのは、既読にしている間に届いた分を落とさないため
（`MissedCallUseCase.markAsRead()` が決める。`NoticeUseCase.clear()` と同じ形）。Reducer は件数を先読みしない。先に 0 にすると、
既読 API が失敗したときに件数が消えたままになる。

**取得のきっかけは画面の切り替え。** HTTP なので黙っていても届かないが、タイマーで
叩き続けるほどの鮮度は要らない。切り替わる節目で取れば足りる。きっかけを決めるのは
`AppNavigation`（遷移を知っているのがそこだけだから）で、マネージャーは呼ばれたら取るだけ。

`refresh()` は実行中の要求があれば何もしない。タブを続けて叩かれたときに、同じ要求が
重なって遅い順に上書きされるのを防ぐため。失敗しても前回の件数を残す。通信が一度
こけただけでバッジが消えると、不在着信を見落とす方に倒れる。

`markAsRead()` は逆に取りやめない。利用者が履歴を見た結果なので、落とすと見たのに
バッジが残る。実行中の取得があれば打ち切る（既読のあとに取り直すので、古い件数で
上書きされるのを防ぐ）。

ViewModel ではなく `@Singleton` なのは、画面をまたいで同じ件数を見せるため。
保持は Hilt の `SingletonComponent` が行う（Application と同じ寿命）。`App` に
手で持たせる必要はない。`IdleTimer` と同じ形。

## 割り切っている点

- API のパスと JSON の形はサンプル用の仮の仕様。使うサーバに合わせて `network/` と `model/` を直す。
  mock flavor では `FakeApiInterceptor` が同じ形の JSON を返す。
- 機器の状態は定期取得なので、着信に気づくまで最大で `pollInterval`（既定 3 秒）遅れる。
  即時性が要るなら、サーバから押し出す仕組み（WebSocket、Server-Sent Events など）に替える。
  その場合も差し替えるのは `DeviceRepositoryImpl.monitor()` の中だけで、`AppStateHolder` から上は変わらない。
- 本物のサーバを `http://` で叩くなら cleartext 許可（Network Security Config）の設定が要る。
  mock flavor は Interceptor が通信の手前で返すので要らない。product の `https://` が
  自己署名証明書なら、その証明書を信頼する設定も要る。
- 着信専用画面は作らず、トップ画面にカードとして出している。
  専用画面にするなら `ui/call/` を 7 ファイルで足し、`IncomingCallRouter` の行き先を変える。
- State の `SavedStateHandle` 保存はしていない。プロセス終了からの復元が要るなら追加する。
- `ProcessLifecycleOwner` の `ON_STOP` は約 700ms 遅れて飛ぶ（構成変更を吸収するため）。
  これより短い他アプリへの移動や、消灯からすぐ復帰した場合は「バックグラウンドに移った」と扱われない。
- プロセスが破棄されてからの再起動は、状態が初期値に戻るためスリープ画面から始まらない。
  それも必ずスリープにしたいなら、`IdleTimer` の `_isSleeping` の初期値を `true` にする。
