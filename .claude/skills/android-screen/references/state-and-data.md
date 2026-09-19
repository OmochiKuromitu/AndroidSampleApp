# 状態の置き場所とデータの足し方

`.claude/skills/android-screen/SKILL.md` の補足。状態をどこに持たせるか迷うとき、
機器（TCP/UDP）や API（HTTP）からのデータを足すとき、UseCase を足すときに読む。

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
- **API から来る** — `model/` にレスポンス型、`data/` のリポジトリが `toDomain()` で変換し、
  結果を `MutableStateFlow` に持って `StateFlow` で公開する（まだ取れていなければ `null`）。
  取り直しは `suspend fun refreshXxx()` で、失敗したら例外を投げて前回の値を残す。
  `AppStateHolder` は通さない。`data/NoticeRepositoryImpl` が見本。
  - ViewModel は `init` で `filterNotNull().collect` して Intent にし、取り直しは `handle` から呼んで
    **失敗したときだけ** Intent で戻す。成功した結果は購読側に流れる。
  - State は「一度でも受け取れたか」のフラグを持ち、読み込み中はそこから決める
    （`ContactState.isLoading` が見本）。取り直しの完了を Intent で待つと、`StateFlow` は
    同じ値を入れ直しても流れないので、結果が前と同じだったときに読み込み中のまま止まる。
- **サーバや機器の文字列（`"COOL"`、`"CONTACT_MISSED"` など）との対応は `data/CodeMapping` に置く。**
  domain の `companion object` に `fromCode` を書かない。domain が通信の言葉を知ると、
  形式が変わったときに domain まで直すことになる。送る向き（`AirconMode.toCode()`）も同じ場所。
- **表示名（「冷房」「来客」など）は domain に持たせず、`ui/common/Labels` の `labelRes()` で
  `@StringRes` に対応させる。** domain は通信の言葉も画面の言葉も知らない状態に保つ。
- 機器へ送るコマンドは `model/CommandRequest` に 1 件足す。文字列化はその型が持つ。

どちらの場合も `domain/repository/` に interface、`domain/usecase/` に UseCase、
`di/RepositoryModule` に `@Binds` を 1 行。ui 層は実装クラスを知らないままにする。

## UseCase の粒度

**呼び出し側から見た 1 つの操作に対して 1 つ。** リポジトリのメソッドを 1 対 1 で
包み直すために作らない。

- よい: `ClearNoticesUseCase` は消去 API、機器側の消去、取り直しを 1 つの操作にまとめる。
  呼び出し側は API が 2 本であることを知らない。
- よい: `RefreshAddressBookUseCase` は電話帳と履歴の 2 本の API をまとめて取り直す。
- 避ける: リポジトリのメソッドを 1 つずつ包んだだけの UseCase を、画面の操作と関係なく並べる。

データを StateFlow で受け取る形なので、1 つの関心事につき「見る」（`ObserveXxxUseCase`、
`StateFlow` の素通し）と「取り直す」（`RefreshXxxUseCase`）の 2 つになるのは想定どおり。
それ以上に割れていたら、まとめられないか疑う。

## flavor で変わる値

接続先、タイムアウト、API のベース URL は `app/build.gradle.kts` の `buildConfigField` と
`config/AppConfig` に置く。アプリ側は `BuildConfig` を直接触らない。
