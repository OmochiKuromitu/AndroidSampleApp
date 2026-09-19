# AGENTS.md

壁付けの操作パネル向け Android アプリ（Jetpack Compose + MVI + Hilt、縦固定、キオスク運用）。
設計の背景と全体像は `README.md`。コードに手を入れる手順は
`.claude/skills/android-screen/SKILL.md`。

## ビルドと確認

```
./gradlew testMockDebugUnitTest     # Reducer などの JVM テスト
./gradlew assembleMockDebug         # Android SDK が必要
```

flavor は `mock`（実機不要。擬似デバイスが接続・エアコン状態・着信を流す）と
`product`（実機接続）。手元で動かすなら **mockDebug**。

**ユニットテストは使い捨てのデータしか触らず、本番にも実機にも触れない。**
実行できる環境なら、テストの実行・変更で壊れた箇所の修正・再実行までを確認なしで進めてよい。

**Android SDK が無い環境もある**（Claude Code on the web など）。その場合ビルドも
テストも実行できない。できないなら「検証していない」と明示して終わること。
通っていないものを通ったように書かない。

## 完了の条件

1. 変更が `SKILL.md` の規約に沿っている（7 ファイル構成、Route と Screen の分離、
   遷移は `AppNavigation` だけ、Reducer は純粋関数）。
2. Reducer を追加・変更したら対応するテストがある。
3. テストが通る（実行できる環境なら）。
4. 構成や規約を変えたら `README.md` とスキルの該当箇所も直した。

満たせないものがあれば、何が残っているかを書いて終える。

## 変更の進め方

- 消した API や引数の呼び出し側は `grep` で洗う。ビルドできない環境では
  コンパイラが教えてくれない。data class に必須引数を足したときが特に危ない。
- コミットは日本語。何をしたかではなく、なぜそうしたかを書く。
- 作業ブランチで進め、`main` へは求められたときだけマージする。

## このリポジトリで決めていること

- 遷移を書くのは `ui/navigation/AppNavigation.kt` だけ。`NavController` を持つのもここだけ。
- `XxxScreen` は表示だけ。ViewModel も Effect も知らない。配線は `XxxRoute`。
- Reducer は `(State, Intent) -> State` の純粋関数。副作用は ViewModel の `handle()`。
- 状態が変わる入口は Reducer だけ。通信結果も共有状態の変化も Intent に変換して通す。

理由は `README.md`、具体的な書き方は `SKILL.md`。
