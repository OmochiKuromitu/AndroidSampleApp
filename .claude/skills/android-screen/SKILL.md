---
name: android-screen
description: このリポジトリの Android コード（app/src/main/java/com/example/androidsampleapp 配下）に画面・タブ・ViewModel・Reducer・遷移・通信を追加または変更するときのファイル構成と規約。Compose + MVI + Hilt の壁付けパネルアプリで、7 ファイル構成と Route/Screen の分離に沿わせる必要がある。そのコードを編集する前に読む。読み方の説明や設計の背景だけを聞かれている場合は README.md を見ればよい。
---

# このリポジトリで画面を作る

壁付けの操作パネル（縦固定、キオスク運用）。機器と TCP で常時つながり、操作は UDP、
通知は HTTP API から取る。全体像と設計理由は `README.md` に書いてある。
このスキルは「手を動かすときに何をどこへ書くか」に絞る。

## まず守ること（これだけで大半の手戻りが防げる）

1. **遷移を書くのは `ui/navigation/AppNavigation.kt` だけ。** `NavController` を持つのもここだけ。
   画面や ViewModel から `navigate` を呼ばない。
2. **`XxxScreen` は表示だけ。** ViewModel も Effect も知らない。配線は `XxxRoute`。
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
| `XxxScreen.kt` | 表示。`state` と `onIntent` だけを受け取る。プレビューもここ |

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

    XxxScreen(state = state, onIntent = viewModel::dispatch, modifier = modifier)
}

// XxxScreen.kt — 表示だけ。ViewModel も Effect も知らない
@Composable
fun XxxScreen(
    state: XxxState,
    onIntent: (XxxIntent) -> Unit,
    modifier: Modifier = Modifier,
) { /* ... */ }

@PanelPreview
@Composable
private fun XxxScreenPreview() {
    PreviewSurface {
        XxxScreen(state = XxxState(/* 見たい状態 */), onIntent = {})
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

## 完了の条件

次を満たしたら終わり。満たせないものがあれば、何が残っているかを明示して終える。

1. 追加・変更したコードが上の「まず守ること」と 7 ファイル構成に沿っている。
2. Reducer を追加または変更したら、対応するテストがある。
3. `./gradlew testMockDebugUnitTest` が通る（実行できる環境なら）。
4. 構成や規約を変えたら `README.md` とこのスキルの該当箇所も直した。

ビルドと実行の確認はこのリポジトリで作業する環境によって可否が変わる。
できない環境なら、検証していないことを伝えて終わる。黙って「動きます」と書かない。
詳細は `AGENTS.md`。

## 詳しい話は必要になってから

この下の 2 つは、該当する作業をするときだけ読む。全部読むと文脈を食うし、
関係のない規約が判断に混じる。

| 読むもの | いつ |
| --- | --- |
| `references/navigation.md` | 画面やタブを増やす、遷移を足す、画面に値を渡す、画面内タブを作る |
| `references/state-and-data.md` | 状態の置き場所に迷う、機器（TCP/UDP）や API（HTTP）の通信を足す |

設計の背景と全体像は `README.md`。このスキルは手を動かすときの手順に絞っている。

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
- **時間に依存するもののテストで `advanceUntilIdle()` を使わない。** タイマーの完了まで
  進んでしまうので、「まだ発火していない」を確かめられない。`runCurrent()` と
  `advanceTimeBy()` を使い分ける（`IdleTimerTest` が見本）。
- **窓口 ViewModel を「画面が必要とするもの」でまとめない。** それは責務ではないので入れる基準が
  立たず、画面が増えるたびに無関係なものが同居する。責務ごとに分ける。

## 変更したら

`README.md` の該当セクションも直す。設計の理由はあちらに書いてあるので、
構成を変えたのに README が古いままだと、次の人が理由の分からない規約に従うことになる。
