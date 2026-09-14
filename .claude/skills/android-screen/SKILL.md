---
name: android-screen
description: このリポジトリ（Jetpack Compose + MVI + Hilt の壁付けパネルアプリ）で画面・タブ・機能を追加、変更、レビューするときの作り方。新しい画面を作る、タブを増やす、ViewModel や Reducer を書く、遷移を足す、API やデバイス通信を足す、状態の置き場所に迷う、といった場面では必ず最初にこれを読むこと。「画面を追加して」「エアコン画面に〜を足して」「通知の一覧を〜」のように機能名だけで依頼された場合も、このリポジトリのコードに触るなら該当する。ファイル構成と命名の規約、遷移の書き方、状態の置き場所の判断基準、過去に踏んだコンパイルエラーの罠が入っている。
---

# このリポジトリで画面を作る

壁付けの操作パネル（縦固定、キオスク運用）。機器と TCP で常時つながり、操作は UDP、
通知は HTTP API から取る。全体像と設計理由は `README.md` に書いてある。
このスキルは「手を動かすときに何をどこへ書くか」に絞る。

## まず守ること（これだけで大半の手戻りが防げる）

1. **遷移を書くのは `ui/navigation/AppNavigation.kt` だけ。** `NavController` を持つのもここだけ。
   画面や ViewModel から `navigate` を呼ばない。
2. **`XxxScreen` は表示だけ。** ViewModel も Intent も Effect も知らない。配線は `XxxRoute`。
   Screen は操作ごとのコールバックを受け取り、それを Intent に変えて `dispatch` するのは Route。
3. **Reducer は純粋関数。** I/O・時刻取得・コルーチン起動を書かない。副作用は ViewModel の `handle()`。
4. **状態が変わる入口は Reducer だけ。** 通信結果も共有状態の変化も Intent に変換して通す。

## 画面を 1 つ足す

`ui/<feature>/` に 7 ファイル。既存の `ui/aircon/` が一番素直な見本なので、迷ったらそれを読む。

| ファイル | 中身 |
| --- | --- |
| `XxxState.kt` | `UiState` を実装した data class |
| `XxxIntent.kt` | `UiIntent` を実装した sealed interface |
| `XxxEffect.kt` | `UiEffect` を実装した sealed interface |
| `XxxReducer.kt` | `(State, Intent) -> State` の純粋関数 |
| `XxxViewModel.kt` | `MviViewModel` を継承。`@HiltViewModel` |
| `XxxRoute.kt` | 配線。ViewModel 取得、State 購読、Effect 受け取り、`LaunchedEffect` |
| `XxxScreen.kt` | 表示。`state` と操作ごとのコールバックだけを受け取る。プレビューもここ |

### 骨組み

```kotlin
// XxxState.kt
data class XxxState(
    val items: List<Item> = emptyList(),
    val isLoading: Boolean = false,
) : UiState

// XxxIntent.kt — 入力の一覧。通信結果も「起きたこと」として Intent にする
sealed interface XxxIntent : UiIntent {
    data object Started : XxxIntent
    data class ItemsChanged(val items: List<Item>) : XxxIntent
    data class ItemClicked(val id: String) : XxxIntent
    data object LoadFailed : XxxIntent
}

// XxxEffect.kt — 一回きりの出来事。遷移の「命令」は書かない（理由は後述）
sealed interface XxxEffect : UiEffect {
    data class ShowMessage(@StringRes val messageRes: Int) : XxxEffect
}

// XxxReducer.kt
class XxxReducer : Reducer<XxxState, XxxIntent> {
    override fun reduce(state: XxxState, intent: XxxIntent): XxxState = when (intent) {
        XxxIntent.Started -> state.copy(isLoading = true)
        is XxxIntent.ItemsChanged -> state.copy(items = intent.items, isLoading = false)
        XxxIntent.LoadFailed -> state.copy(isLoading = false)
        is XxxIntent.ItemClicked -> state   // 状態は変えない。Effect で扱う
    }
}

// XxxViewModel.kt
@HiltViewModel
class XxxViewModel @Inject constructor(
    observeItems: ObserveItemsUseCase,
    private val doSomething: DoSomethingUseCase,
) : MviViewModel<XxxState, XxxIntent, XxxEffect>(
    initialState = XxxState(),
    reducer = XxxReducer(),
) {
    init {
        // 共有状態の変化も Intent に変換して Reducer に通す
        viewModelScope.launch {
            observeItems().collect { dispatch(XxxIntent.ItemsChanged(it)) }
        }
        dispatch(XxxIntent.Started)
    }

    override suspend fun handle(intent: XxxIntent, previous: XxxState, current: XxxState) {
        when (intent) {
            is XxxIntent.ItemClicked -> runCatching { doSomething(intent.id) }
                .onFailure { sendEffect(XxxEffect.ShowMessage(R.string.command_failed)) }

            XxxIntent.Started,
            is XxxIntent.ItemsChanged,
            XxxIntent.LoadFailed,
            -> Unit
        }
    }
}
```

`handle()` が `previous` と `current` の両方を受け取るのは、「またいだ瞬間の 1 回だけ」を
表現するため。例: `ui/sleep/SleepViewModel` はスワイプの進み具合が 1.0 に達した瞬間だけ
Effect を出し、指がさらに動いても重ねて送らない。

```kotlin
// XxxRoute.kt — AppNavigation から呼ばれる入口
@Composable
fun XxxRoute(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: XxxViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is XxxEffect.ShowMessage ->
                snackbarHostState.showSnackbar(context.getString(effect.messageRes))
        }
    }

    // 操作を Intent に変えるのは Route。Screen に Intent を渡さない
    XxxScreen(
        state = state,
        onItemClick = { viewModel.dispatch(XxxIntent.ItemClicked(it)) },
        modifier = modifier,
    )
}

// XxxScreen.kt — 表示だけ。ViewModel も Intent も Effect も知らない
@Composable
fun XxxScreen(
    state: XxxState,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) { /* ... */ }

@PanelPreview
@Composable
private fun XxxScreenPreview() {
    PreviewSurface {
        XxxScreen(state = XxxState(/* 見たい状態 */), onItemClick = {})
    }
}
```

Route と Screen を分けるのは、Screen を表示だけに保つため。副作用の配線が混ざると、
見た目を直すつもりでライフサイクルの都合を読む羽目になる。加えて `hiltViewModel()` は
プレビューで解決できないので、State を引数で渡せる Screen 側でないとプレビューが描けない。

### プレビュー

`@PanelPreview` を付けると縦長サイズでライトとダークが並ぶ。`PreviewSurface` で包むのを
忘れないこと。包まないと `Dimensions` とタイポグラフィが既定値になり、実機と違う見た目のまま
調整してしまう。異常系（未接続、送信中、取得失敗）も 1 つずつ出しておくと、実機で
再現しづらい状態を目で確認できる。MVI は State が 1 つの data class なので、そこが安い。

## 遷移を足す

1. `ui/common/Route.kt` にルート文字列を 1 行。
2. タブなら `ui/main/MainState.kt` の `MainTab` に 1 行（ルートは `Route.XXX` を渡す）。
3. `AppNavigation` の `NavHost` に `composable(Route.XXX) { ... }` を 1 つ。

下部バーを出す画面は `MainRoute` で包む。選択状態にするタブは `MainTab.fromRoute(Route.XXX)`
で引くこと。`MainTab.XXX` と直接書かないのは、遷移に使う値の出どころを `Route` に
揃えておくため。

### 画面に値を渡す

ルートに引数を付ける。受け取るのは ViewModel の `SavedStateHandle`。
見本は `ui/contact`（通知から不在着信で飛ぶと履歴タブで開く）。

1. `Route` に引数名と、引数を含むパターン（`"contact?list={list}"`）、ルートを組み立てる関数。
2. `AppNavigation` の `composable` に `arguments = listOf(navArgument(...) { ... })`。
   省略可能にするなら `nullable = true` と `defaultValue = null`。
3. ViewModel のコンストラクタで `savedStateHandle` から読み、**`initialState` で解決する**。
   Intent にすると、取得が終わる前に既定の表示が一瞬見えてから切り替わる。
4. 引数つきのルートへ飛ぶときは `restoreState` を使わない。復元すると保存済みの
   エントリがそのまま戻り、新しく渡した引数が無視される。

飛び先によって伴う情報が違うなら、飛び先の型を `enum` ではなく `sealed interface` にする。
ドメインは事実（不在着信があった）だけを持ち、「だからどのタブを開くか」は ui 層が決める。

### 画面の中のタブ

遷移を伴わないタブ（連絡先の電話帳 / 履歴）はルートを増やさない。どれを出しているかは
画面の状態なので `XxxState` に持たせ、`TabRow` で出し分ける。データは開いたときに
まとめて取り、タブを触るたびに通信しない。

画面から遷移したいときは、ViewModel が **「何が起きたか」** を Effect で返し、
`AppNavigation` が行き先を決める。

```kotlin
// よい: 出来事の報告
sealed interface SleepEffect : UiEffect {
    data object Unlocked : SleepEffect
    data class NoticeSelected(val destination: NoticeDestination) : SleepEffect
}

// 避ける: 遷移の命令
data class NavigateToAircon(...) : SleepEffect
```

命令にすると、行き先の判断が画面ごとに散る。報告にしておけば「この出来事が起きたら
どこへ行くか」が `AppNavigation` だけを読めば分かる。

## 状態をどこに置くか

判断の目安は「全画面が見るか」ではなく **書き手と読み手の数**。

| 置き場所 | 何を置くか | 書き手 |
| --- | --- | --- |
| `XxxState` | 画面固有（送信中フラグ、入力中の値、表示の進み具合） | その画面の Reducer |
| `core/AppStateHolder` | 機器から TCP で降ってくる状態（接続、着信、エアコン、機器からの通知） | `data/DeviceRepositoryImpl` だけ |
| 専用の `@Singleton` | 画面をまたいで共有するもの（`IdleTimer` のスリープ状態、`MissedCallManager` の不在着信件数） | そのクラス自身 |

**読み手が 1 画面なら共有の器を作らない。** その画面の `XxxState` に持たせる。
読み手が 2 か所以上になった時点で `@Singleton` に引き上げる。
`@Singleton` の保持先は Hilt の `SingletonComponent`（Application と同じ寿命）なので、
`App` に手で持たせる必要はない。

HTTP のように能動的に取りに行くものは、取得のきっかけを `AppNavigation` が決め、
マネージャーは呼ばれたら取るだけにする。各画面がそれぞれ叩くと、画面が増えるたびに
取得のタイミングが散る。実行中の要求が重ならないよう `refresh()` 側で間引くこと。
見本は `core/MissedCallManager`。

共有の器は書き手が複数いて初めて元が取れる。書き手が 1 つなら、そのクラスに持たせる。
過去に「全画面が見るから」で `AppStateHolder` にスリープ状態を入れて、状態を持つ場所と
更新を決める場所が分かれてしまい、`IdleTimer` を読むだけでは挙動が追えなくなった。

## データを足す

機器（TCP/UDP）と API（HTTP）で経路が違う。混ぜない。
画面で 1 つの一覧に合わせたいときも、取り込みまでは別々に通し、画面の `XxxState` に
出どころ別に持たせて合わせる（見本は `SleepState` の `apiNotices` / `deviceNotices` / `notices`）。
並べ替えるなら時刻は比べられる値（epoch ミリ秒）で持ち、一覧の key が出どころ間で重ならないようにする。

- **機器から来る** — `network/MessageParser` に解釈を足し、`model/DeviceMessage` に型を足し、
  `data/DeviceRepositoryImpl` が `AppStateHolder` に反映する。
- **API から来る** — `model/` にレスポンス型、`data/` のリポジトリが自分で保持して
  `toDomain()` で変換。`AppStateHolder` は通さない。`data/NoticeRepositoryImpl` が見本。
- **サーバや機器の文字列（`"COOL"`、`"CONTACT_MISSED"` など）との対応は `data/CodeMapping` に置く。**
  domain の `companion object` に `fromCode` を書かない。domain が通信の言葉を知ると、
  形式が変わったときに domain まで直すことになる。送る向き（`AirconMode.toCode()`）も同じ場所。
- 機器へ送るコマンドは `model/CommandRequest` に 1 件足す。文字列化はその型が持つ。

どちらの場合も `domain/repository/` に interface、`domain/usecase/` に UseCase、
`di/RepositoryModule` に `@Binds` を 1 行。ui 層は実装クラスを知らないままにする。

### UseCase の粒度

**呼び出し側から見た 1 つの操作に対して 1 つ。** リポジトリのメソッドを 1 対 1 で
包み直すために作らない。

- よい: `ClearNoticesUseCase` は消去 API を呼んでから取得 API を呼び、新しい一覧を返す。
  呼び出し側は「消した結果の一覧が返る」とだけ知っていればよく、API が 2 本であることを知らない。
- 避ける: `RefreshNoticesUseCase`（取りに行く）と `ObserveNoticesUseCase`（結果を見る）に
  分ける。同じ 1 つの関心事が 2 つに割れて、ViewModel が両方を注入する羽目になる。

ViewModel が 3 つ以上 UseCase を注いでいたら、割りすぎを疑う。
なお `ObserveXxxUseCase` のように `StateFlow` を素通しするだけのものは、
機器の共有状態（接続、着信、エアコン）のように複数画面が同じものを見る場合に限る。

flavor で変わる値（接続先、タイムアウト、API のベース URL）は
`app/build.gradle.kts` の `buildConfigField` と `config/AppConfig` に置く。
アプリ側は `BuildConfig` を直接触らない。

## テスト

Reducer と `MessageParser` は Android に依存しない純粋な処理なので JVM テストで完結する。
新しい Reducer を書いたら、最低限「状態が変わる分岐」と「変わらない分岐」を 1 本ずつ。

```
./gradlew testMockDebugUnitTest
```

時間に依存するもの（`IdleTimer`）は `runTest` の仮想時間で書く。
`runCurrent()` と `advanceTimeBy()` を使い分けること。`advanceUntilIdle()` は
タイマーの完了まで進んでしまうので、「まだ発火していない」を確かめたいときに使えない。

## 踏んだ罠（同じことを繰り返さない）

- **KDoc に `path/*.kt` のようなグロブを書かない。** Kotlin のブロックコメントはネストするので、
  `/*` が内側のコメントを開き、`*/` がそれを閉じてファイル末尾まで飲み込む。バッククォートで囲むか書かない。
- **トレイリングラムダは最後の引数に束縛される。** `viewModel` のような既定値つき引数が
  最後にある Composable を `Foo { ... }` と呼ぶと、ラムダがそちらへ渡る。名前付き引数で呼ぶ。
- **Composable に `@Inject` はできない。** 必要なものは `hiltViewModel()` で取る窓口 ViewModel を作る
  （`ui/navigation/IdleTimerViewModel` が見本）。ただし Application から触る必要があるものは
  `@Singleton` にしないと届かない。
- **data class に必須引数を足したら呼び出し側を全部確認する。** `AppConfig` に 1 つ足して
  テストの組み立てを直し忘れ、テストのコンパイルが落ちた。
- **窓口 ViewModel を「画面が必要とするもの」でまとめない。** それは責務ではないので入れる基準が
  立たず、画面が増えるたびに無関係なものが同居する。責務ごとに分ける。

## 変更したら

`README.md` の該当セクションも直す。設計の理由はあちらに書いてあるので、
構成を変えたのに README が古いままだと、次の人が理由の分からない規約に従うことになる。
