# 遷移とルート

画面・タブを増やすとき、画面に値を渡すとき、画面内タブを作るときに読む。
遷移そのものを触らないなら読む必要はない。

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

