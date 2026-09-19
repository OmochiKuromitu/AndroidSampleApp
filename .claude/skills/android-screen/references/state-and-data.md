# 状態とデータ

状態の置き場所に迷ったとき、通信（機器の TCP/UDP、API の HTTP）を足すときに読む。
既にある画面の表示だけを直すなら読む必要はない。

## 状態をどこに置くか

判断の目安は「全画面が見るか」ではなく **書き手と読み手の数**。

| 置き場所 | 何を置くか | 書き手 |
| --- | --- | --- |
| `XxxState` | 画面固有（送信中フラグ、入力中の値、表示の進み具合） | その画面の Reducer |
| `core/AppStateHolder` | 機器から TCP で降ってくる状態（接続、着信、エアコン） | `data/DeviceRepositoryImpl` だけ |
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

- **機器から来る** — `network/MessageParser` に解釈を足し、`model/DeviceMessage` に型を足し、
  `data/DeviceRepositoryImpl` が `AppStateHolder` に反映する。
- **API から来る** — `model/` にレスポンス型、`data/` のリポジトリが自分で保持して
  `toDomain()` で変換。`AppStateHolder` は通さない。`data/NoticeRepositoryImpl` が見本。
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

