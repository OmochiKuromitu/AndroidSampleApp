# AndroidSampleApp

Jetpack Compose + MVI (Model-View-Intent) の Android サンプル。
Navigation bar で 3 タブ（ホーム / 検索 / プロフィール）を切り替え、タブ切り替えも
Intent → Reducer を通す構成にしてある。

## 動かし方

Android Studio でこのディレクトリを開くだけ。追加設定は不要。

- compileSdk / targetSdk: 35, minSdk: 24
- Android Gradle Plugin 8.7.3 / Kotlin 2.0.21 / Gradle 8.11.1
- Compose BOM 2024.12.01, Navigation Compose 2.8.5

Android Studio が新しい場合、AGP のアップグレード提案が出ることがある。従って問題ない。

## データの流れ

```
     ┌──────────────── UiState ────────────────┐
     │                                          │
     ▼                                          │
 Composable ──dispatch(UiIntent)──▶ MviViewModel ┤
     ▲                                  │       │
     │                                  ├── Reducer(state, intent) -> state
     └────collect(UiEffect)─────────────┘       │
                                    handle(...) ┘  ← I/O・遷移命令・追加 Intent
```

- **State**: 画面が描画に使う唯一の入力。不変の data class。
- **Intent**: 状態を変えうる入力すべて。ユーザー操作だけでなく、通信結果（`TasksLoaded`）も Intent として Reducer に戻す。
- **Reducer**: `(State, Intent) -> State` の純粋関数。I/O・時刻取得・コルーチン起動を書かない。
- **Effect**: 状態として保持すべきでない一回きりの出来事（画面遷移、スナックバー）。

State と Effect を分けるのが要点。「画面遷移」を State に持たせると、
画面回転や再生成のたびに同じ遷移が再実行される。

## 画面遷移の扱い

| 対象 | 表現 | 理由 |
| --- | --- | --- |
| 選択中のタブ | `RootState.selectedTab`（State） | 再生成後も復元されるべき値。Navigation bar のハイライトはここだけを見る |
| タブへの遷移 | `RootEffect.NavigateToTab`（Effect） | 一回きりの命令 |
| 同じタブの再タップ | `RootEffect.PopToTabRoot`（Effect） | 状態は変わらないが動作はある |
| 戻るキーでの移動 | `RootIntent.BackStackChanged`（Intent） | NavController が先に動いた結果を State に追従させるだけ。ここから再遷移するとループする |

つまりタップは必ず `RootIntent.TabClicked` として Reducer を通り、
その結果として Effect が navigate を呼ぶ。UI から直接 `navController.navigate` は呼ばない。

## ディレクトリ

```
app/src/main/java/com/example/androidsampleapp/
├── MainActivity.kt
├── core/mvi/            MVI の土台（UiState / UiIntent / UiEffect / Reducer / MviViewModel / CollectEffect）
├── data/                Task, TaskRepository（インメモリ。通信や DB に差し替える前提）
├── di/AppGraph.kt       手書きの依存グラフ。大きくなったら Hilt に置き換える
├── navigation/          TopLevelDestination（タブ定義）, Routes, AppNavHost
├── root/                Scaffold + Navigation bar。タブ切り替えの MVI
├── feature/home/        一覧 → 詳細（タブ内遷移）
├── feature/search/      入力の debounce と、古い結果の破棄
├── feature/profile/     設定トグルと保存 Effect
└── ui/theme/
```

機能を 1 つ足すときは `feature/<name>/` に `Contract` / `Reducer` / `ViewModel` / `Screen` の 4 ファイル、
タブを 1 つ足すときは `TopLevelDestination` に 1 行と `AppNavHost` に `composable` を 1 つ。

## テスト

Reducer は純粋関数なので、Android 依存なしの JVM テストで完結する。

```
./gradlew test
```

`app/src/test/` に Home / Search / Root の Reducer テストを置いてある。
ViewModel まで含めて検証したい場合は `kotlinx-coroutines-test` の `runTest` を使う（依存は追加済み）。

## 割り切っている点

- DI は `AppGraph`（手書き）。実務では Hilt を推奨。
- Repository はインメモリ + `delay` のスタブ。
- `SavedStateHandle` による State の保存はしていない（プロセス終了からの復元が必要なら追加する）。
