# AndroidSampleApp

Jetpack Compose + MVI の Android アプリ。壁付けの操作パネルを想定していて、
機器と TCP で常時つながり、操作は UDP で送る。無操作が続けばスリープ画面に落ちる。

- 下部バーで 3 タブ（トップ / エアコン / スリープ）
- 画面遷移も含めて Intent → Reducer を通す
- flavor は `mock`（実機不要）と `product`（実機接続）

## ビルド

壁付けパネル想定のため **縦固定**（`android:screenOrientation="portrait"`）。
プレビューの寸法も縦長にしてある。

Android Studio でこのディレクトリを開く。実行構成は 4 つ（mock/product × debug/release）。
実機が手元に無いときは **mockDebug** を選ぶ。擬似デバイスが接続・エアコン状態・着信を流す。

- compileSdk / targetSdk 35, minSdk 24
- AGP 8.7.3 / Kotlin 2.0.21 / Gradle 8.11.1 / Hilt 2.52 (KSP)
- Compose BOM 2024.12.01, Navigation Compose 2.8.5

---

## アーキテクチャ

### レイヤと依存の向き

```
        ui  ──────▶  domain  ◀──────  data
        │            (interface)        │
        │                               ├─▶ network   (TCP / UDP)
        └─▶ core (AppStateHolder, MVI の土台)
                     ▲                  │
                     └──────────────────┘
                       共有状態への書き込みは data / IdleTimer だけ

        service ─▶ domain.DeviceRepository   常時監視の寿命管理
        di      ─▶ interface と実装の結線
        config  ─▶ flavor 由来の設定（BuildConfig の読み口）
```

矢印は依存の向き。**ui は data / network を知らない**。domain の interface と UseCase だけを見る。
実装の差し替えは `di/RepositoryModule` の `@Binds` 2 行で完結する。

### MVI のデータの流れ

```
     ┌──────────────── State ────────────────┐
     │                                        │
     ▼                                        │
 Composable ──dispatch(Intent)──▶ ViewModel ──┤
     ▲                               │        │
     │                               ├── Reducer(state, intent) -> state   純粋関数
     └────collect(Effect)────────────┘        │
                             handle(...) ─────┘  UseCase 呼び出し・Effect 送出・追加 dispatch
```

- **State** — 画面が描画に使う唯一の入力。不変の data class。
- **Intent** — 状態を変えうる入力すべて。ユーザー操作に限らず、
  機器からの通知（`AirconChanged`）や送信結果（`CommandSucceeded`）も Intent にして戻す。
  こうすると状態が変わる経路が Reducer 1 か所に収まる。
- **Reducer** — `(State, Intent) -> State`。I/O・時刻取得・コルーチン起動を書かない。
- **Effect** — 状態として持つべきでない一回きりの出来事（遷移、スナックバー）。

State と Effect を分けるのが要点。遷移を State に持たせると、画面回転などの再生成のたびに
同じ遷移が走る。逆に「選択中のタブ」を Effect にすると、復帰したときに復元できない。

### ui/<feature> の 6 ファイル

画面 1 つにつき、必ずこの 6 つを置く。ファイル名で役割が分かる状態を保つ。

| ファイル | 中身 |
| --- | --- |
| `XxxState.kt` | 画面の状態。`UiState` を実装した data class |
| `XxxIntent.kt` | 入力の一覧。`UiIntent` を実装した sealed interface |
| `XxxEffect.kt` | 一回きりの出来事。`UiEffect` を実装した sealed interface |
| `XxxReducer.kt` | `(State, Intent) -> State` の純粋関数 |
| `XxxViewModel.kt` | `MviViewModel` を継承。`handle()` に副作用を隔離 |
| `XxxScreen.kt` | Composable。State を描き、Intent を投げ、Effect を受ける |

`XxxScreen.kt` の中はさらに 2 つに割ってある。

- `XxxScreen` — `hiltViewModel()` で ViewModel を取り、Effect を受ける。配線だけ。
- `XxxContent` — `state` と `onIntent: (Intent) -> Unit` だけを受け取る private な Composable。

割ってあるのはプレビューのため。`hiltViewModel()` はプレビューでは解決できないので、
`XxxScreen` そのものは描画できない。状態を引数で渡せる `XxxContent` に
`@PanelPreview` を付けて、State を差し替えながら見た目を確認する。

```kotlin
@PanelPreview
@Composable
private fun AirconContentOfflinePreview() {
    PreviewSurface {
        AirconContent(
            state = AirconState(connectionState = ConnectionState.DISCONNECTED),
            onIntent = {},
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

土台は `core/mvi/` にある（`Mvi.kt` / `MviViewModel.kt` / `CollectEffect.kt`）。
`MviViewModel` は Intent を 1 本のチャネルに集約し、到着順に reduce する。
副作用だけは並行に走らせて、長い I/O が後続 Intent の reduce を止めないようにしている。

### 画面遷移の扱い

| 対象 | 表現 | 理由 |
| --- | --- | --- |
| 選択中のタブ | `MainState.selectedTab`（State） | 再生成後も復元されるべき値。下部バーのハイライトはここだけを見る |
| タブへの遷移 | `MainEffect.NavigateToTab`（Effect） | 一回きりの命令 |
| 同じタブの再タップ | `MainEffect.PopToTabRoot`（Effect） | 状態は変わらないが動作はある |
| スリープへ | `MainEffect.NavigateToSleep`（Effect） | タブではないので `selectedTab` を動かさない。復帰時に元のタブへ戻る |
| 戻る操作での移動 | `MainIntent.BackStackChanged`（Intent） | NavController が先に動いた結果を State に追従させるだけ。ここから再遷移するとループする |

タップは必ず `MainIntent.TabClicked` として Reducer を通り、その結果の Effect が navigate を呼ぶ。
Composable から直接 `navController.navigate` は呼ばない。

NavHost は 2 段になっている。外側（`ui/navigation/AppNavigation`）が `main` と `sleep`、
内側（`ui/main/MainScreen`）がタブの中身。下部バーの有無で階層を分けている。

### 共有状態 — AppStateHolder

接続状態・着信・エアコンの現在値は、全画面が見る。これを `core/AppStateHolder` が単独で持つ。

- **書き込むのは 1 か所だけ** — 受信を反映する `data/DeviceRepositoryImpl`。
- 画面と ViewModel は読むだけ。UseCase 経由で `StateFlow` を受け取り、
  変化を Intent に変換して自分の Reducer に流す。
- 画面固有の状態（送信中フラグなど）はここに置かず、各 `XxxState` が持つ。

ここに置くのは **機器から降ってくる状態** に限る。「全画面が見る値だから」で
何でも入れると、書き手が増えて所在が追えなくなる。判断の目安は書き手と読み手の数。

スリープ中かどうかがその例で、これは `AppStateHolder` ではなく `IdleTimer` が自分で持つ。
書き手は `IdleTimer` だけ、読み手も `AppNavigation` だけなので、共有の器を通す理由がない。
通すと、状態を持つ場所と更新を決める場所が分かれてしまい、
`IdleTimer` を読むだけではスリープの挙動が追えなくなる。

### スリープに入る条件

状態と判断は `ui/navigation/IdleTimer` が一手に持ち、`isSleeping` を立てるだけ。
遷移は、それを見た `AppNavigation` が行う。きっかけは 4 つ。

| きっかけ | 呼ぶもの | 挙動 |
| --- | --- | --- |
| 無操作が続いた | 内部タイマー（`AppConfig.sleepTimeout`） | タイムアウトでスリープ |
| バックグラウンドに移った | `IdleTimer.onEnteredBackground()` | 無操作時間に関係なく即スリープ |
| 画面が消えた（電源ボタン / 消灯タイムアウト） | `IdleTimer.onScreenOff()` | 復帰後もスリープ画面から始まる |
| スリープタブが選ばれた | `IdleTimer.onSleepRequested()` | 即スリープ |

操作の検知は `AppNavigation` のルートに置いた `pointerInput` が担う。
`PointerEventPass.Initial` で子より先に覗くだけなので、画面側の操作は妨げない。

`IdleTimer` の入口は 2 つに分かれている。

- `onInteraction()` — 無操作タイマーを測り直す。**スリープ中は無視する。**
- `wake()` — スリープを解除する。解除操作と着信からだけ呼ぶ。

触れただけで解除されると、下で述べる解除ジェスチャを定義した意味がなくなる。
判断を `IdleTimer` に閉じ込めてあるので、画面側は「触られた」と伝えるだけでよい。

きっかけの検知は `App` が 2 系統でやっている。

- **他アプリへの移動** — `ProcessLifecycleOwner` の `ON_STOP`。
  Activity の `onStop` を使わないのは、構成変更（画面サイズ、ロケール、
  ダークテーマの切り替えなど）による再生成でも呼ばれてしまうから。
  `ProcessLifecycleOwner` は構成変更を除外する。
- **画面消灯** — `ACTION_SCREEN_OFF` のブロードキャスト。

キオスク（Device Owner + LockTask）で電源ボタンを押して消灯し、すぐ復帰する経路は
`ON_STOP` だけでは取りこぼす。`ON_STOP` は構成変更を吸収するため約 700ms 遅れて飛び、
その間に復帰すると打ち消されるため。`ACTION_SCREEN_OFF` は遅延なく届くので、
この経路はこちらで拾う。なおこのアクションは manifest 登録では受け取れないので、
`App.onCreate` で実行時に登録している。

復帰したときではなく離れたときに倒しているのは、復帰時に判定すると遷移が走るまでの
1 フレームだけ前の画面が見えることがあるため。離れる時点で倒しておけば、
戻ってきた最初の描画がスリープ画面になる。

### スリープの解除

画面下端の帯を上にスワイプしたときだけ解除する。タップでは解除しない。
拭き掃除や誤接触で操作画面に戻らないようにするため。

| 項目 | 既定値 | 置き場所 |
| --- | --- | --- |
| 受け付ける帯の高さ | 50dp | `Dimensions.unlockAreaHeight` |
| 解除に必要な移動量 | 120dp | `Dimensions.unlockDistance` |

帯より移動量が大きいのは矛盾ではない。ドラッグは始まった位置で受け付けが決まり、
その後は帯の外へ出ても追跡が続く。帯は「どこから始めたら解除操作とみなすか」だけを決める。

指の移動量は画面側で 0f..1f に正規化し、`SleepIntent.UnlockDragged` として
Reducer に渡す。`SleepState.unlockProgress` がそれを保持し、ヒント表示が
その値に応じて持ち上がって濃くなる。押し戻せば進み具合も戻るので、途中でやめられる。

解除は `SleepViewModel.handle()` が `previous` と `current` を見比べ、
進み具合が 1.0 に達した瞬間の 1 回だけ `SleepEffect.Wake` を出す。
指がさらに動いても重ねて送らない。

### スリープ画面の通知一覧

時刻表示の下に、受け取った通知を出す。消去ボタンは一覧の右上に小さく置く。

通知は **HTTP の API** から取る。機器との TCP とは経路が別なので、
`AppStateHolder`（機器から降ってくる状態）は通らず、`NoticeRepositoryImpl` が
自分で保持する。

```
SleepViewModel.init ─▶ SleepIntent.Started
                        └▶ RefreshNoticesUseCase ─▶ NoticeRepositoryImpl.refresh()
                                                     └▶ API（未実装。今は仮データ）
                                                         └▶ notices: StateFlow が更新され、
                                                             NoticesChanged として画面に戻る
```

取得と消去の結果はどちらも `notices` の `StateFlow` を通って戻る。画面は
`ObserveNoticesUseCase` を購読するだけで、経路が 1 本に保たれる。

スリープに入るたびに ViewModel ごと作り直されるので、取得もそのたびに走る。
失敗しても明示的な再試行ボタンは置いていない（次にスリープへ入れば取り直す）。

**API はまだサーバ側が無い。** `NoticeRepositoryImpl.fetchFromApi()` が仮データを返しており、
`TODO` を付けてある。差し替えるのはこのクラスの中だけで、UseCase から上は変わらない。
エンドポイントは `AppConfig.apiBaseUrl`（flavor ごとに `buildConfigField` で設定）。

**飛び先は通知自身が持つ。**

```kotlin
data class Notice(
    val id: String,
    val category: NoticeCategory,   // CALL / AIRCON / ALERT / INFO。一覧ではタグとして色分け
    val message: String,
    val destination: NoticeDestination,   // TOP / AIRCON
)
```

一覧側に「この分類ならここ」という対応表を持たせない。飛び先を増やすときは
`NoticeDestination` と、そのタブへの対応（`toMainTab()`）だけを触ればよい。
ドメインはルート文字列を知らず、変換は `ui/main` に置いてある。

タップしたときの経路はこうなる。

```
NoticeClicked ─▶ Reducer（状態は変えない）
              └▶ handle() ─▶ SleepEffect.OpenDestination
                              └▶ AppNavigation が requestedTab に控えて idleTimer.wake()
                                  └▶ 復帰してメイン画面が composed され、
                                      MainIntent.TabClicked として消化される
```

タブは `MainViewModel` の状態で、`AppNavigation` からは直接触れない。そのため
「どのタブを出したいか」だけを引数で渡し、受け取った側が 1 度だけ処理して消す。

なお通知のタップは、下端スワイプを経ずに解除される唯一の経路になる。
意図した操作なので許しているが、誤接触も通してしまう点は承知のうえ。

### 常時監視

```
MonitoringService (前面サービス)
  └─ DeviceRepository.monitor()          再接続ループ。切れたら待って繋ぎ直す
       └─ TcpClient.connect(): Flow      1 行 = 1 メッセージ
            └─ MessageParser.parse()     文字列 -> DeviceMessage
                 └─ AppStateHolder       接続状態・着信・エアコンを更新

操作: ViewModel -> UseCase -> Repository -> UdpCommandClient.send(CommandRequest)
      結果は TCP 側の通知で返ってくるので、送信時に楽観的な更新はしない
```

サービスは寿命の管理だけを持ち、再接続の判断は Repository 側にある。
`TcpClient` の `readLine()` は割り込めないので、`soTimeout` で定期的に制御を戻し、
コルーチンのキャンセルを見られるようにしている。

---

## ディレクトリ

```
app/src/main/java/com/example/androidsampleapp/
├── App.kt                  @HiltAndroidApp
├── MainActivity.kt
├── config/                 flavor に対応した設定（AppConfig）
├── core/                   AppStateHolder — 全画面共有状態の単一管理者
│   └── mvi/                UiState / UiIntent / UiEffect / Reducer / MviViewModel / CollectEffect
├── di/                     Hilt モジュール（AppModule / RepositoryModule / Qualifiers）
├── service/                MonitoringService — TCP の常時監視
├── domain/
│   ├── model/              Aircon / ConnectionState / IncomingCall / Notice
│   ├── repository/         DeviceRepository / AirconRepository（interface）
│   └── usecase/            ObserveXxx / SetXxx / AnswerCall …
├── data/                   Repository 実装
├── network/                TcpClient / MessageParser / UdpCommandClient
├── model/                  DeviceMessage（受信）/ CommandRequest（送信）/ MasterData
└── ui/
    ├── navigation/         AppNavigation / IncomingCallRouter / IdleTimer
    ├── common/             Route / AppHeader / NoticeList / 共通コンポーネント / プレビュー定義
    ├── theme/              Color / Type / Dimensions / Theme
    ├── main/               BottomNaviBar とメイン画面（MVI 6 ファイル + BottomNaviBar）
    ├── top/                メイン画面に入れる画面（MVI 6 ファイル）
    ├── aircon/             エアコン操作（MVI 6 ファイル）
    └── sleep/              スリープ画面（MVI 6 ファイル）
```

`model/` と `domain/model/` の使い分け:

- `model/` — 通信の語彙。受信した 1 行の解釈結果、送るコマンド、API のレスポンス、
  機器仕様の固定値。
- `domain/model/` — アプリが扱う語彙。画面と UseCase はこちらだけを見る。

変換は data 層（`DeviceRepositoryImpl`）が担う。プロトコルが変わっても影響を network と data に閉じる。

### 足すとき

- **画面を 1 つ足す** — `ui/<name>/` に 6 ファイル。ViewModel は `@HiltViewModel`。
- **タブを 1 つ足す** — `MainTab` に 1 行と `MainScreen` の NavHost に `composable` を 1 つ。
- **機器の機能を 1 つ足す** — `model/CommandRequest` にコマンド、`MessageParser` に解釈、
  `domain/repository` に口、`data` に実装、`domain/usecase` に UseCase。

## テスト

Reducer と MessageParser は Android に依存しない純粋な処理なので、JVM テストで完結する。

```
./gradlew testMockDebugUnitTest
```

`app/src/test/` に Main / Top / Aircon の Reducer と MessageParser のテストがある。
ViewModel まで含めて検証する場合は `kotlinx-coroutines-test` の `runTest` を使う（依存は追加済み）。

## 割り切っている点

- 通信プロトコルは `|` 区切りのテキストという仮のもの。実仕様に合わせて
  `MessageParser` と `CommandRequest` を差し替える。
- 通知取得 API は未実装。`NoticeRepositoryImpl` が仮データを返している。
  実装時は HTTP クライアント（Retrofit / Ktor など）の依存追加と、
  mock flavor の `http://` を叩くなら cleartext 許可の設定が要る。
- 着信専用画面は作らず、トップ画面にカードとして出している。
  専用画面にするなら `ui/call/` を 6 ファイルで足し、`IncomingCallRouter` の行き先を変える。
- State の `SavedStateHandle` 保存はしていない。プロセス終了からの復元が要るなら追加する。
- 通知権限（Android 13 以降の `POST_NOTIFICATIONS`）の実行時リクエストは未実装。
  権限が無いと前面サービスの通知が出ないだけで、監視自体は動く。
- `ProcessLifecycleOwner` の `ON_STOP` は約 700ms 遅れて飛ぶ（構成変更を吸収するため）。
  これより短い他アプリへの移動は「バックグラウンドに移った」と扱われない。
  画面消灯の経路は `ACTION_SCREEN_OFF` で拾うのでこの影響を受けない。
- プロセスが破棄されてからの再起動は、状態が初期値に戻るためスリープ画面から始まらない。
  それも必ずスリープにしたいなら、`IdleTimer` の `_isSleeping` の初期値を `true` にする。
