
```kotlin

/**
 * メイン画面の行き先。どのタブを開き、タブの中で何を出すかを 1 つの値で表す。
 * nav 引数はこれ 1 つだけ。タブごとに引数を分けると、あり得ない組み合わせまで表せてしまう。
 *
 * 行き先を増やすとき:
 * 1. ここに 1 件足す（@Serializable と @SerialName を付ける。
 *    @SerialName が無いとクラス名が引数の文字列になり、名前を変えたときに読めなくなる）
 * 2. MainScreen の when に分岐を足す
 * 3. 通知から飛べる行き先なら、NoticeDestination と toMainDestination() に足す
 *
 * 2 と 3 は when の網羅チェックで、足し忘れるとコンパイルエラーになる。1 だけ足して終わることはない。
 */
@Serializable
sealed interface MainDestination {
    val tab: MainTab

    @Serializable data object Top : MainDestination { override val tab get() = MainTab.TOP }
    @Serializable data object Aircon : MainDestination { override val tab get() = MainTab.AIRCON }
    @Serializable data class Contact(val list: ContactList = ContactList.DEFAULT) : MainDestination {
        override val tab get() = MainTab.CONTACT
    }
}

// 今の NoticeDestination.toRoute() の置き換え。変換の置き場所は変わらない
fun NoticeDestination.toMainDestination(): MainDestination = when (this) {
    NoticeDestination.Top -> MainDestination.Top
    NoticeDestination.Aircon -> MainDestination.Aircon
    is NoticeDestination.Contact ->
        MainDestination.Contact(if (hasMissedCall) ContactList.HISTORY else ContactList.DEFAULT)
}
```

nav 引数への出し入れは 1 か所に閉じる。kotlinx.serialization は既に入っているので Json に任せれば、
行き先を増やしても encode/decode は触らなくて済む。

```kotlin
object Route {
    object Main {
        const val ARG_DEST = "dest"
        const val path = "main?$ARG_DEST={$ARG_DEST}"
        fun of(dest: MainDestination) = "main?$ARG_DEST=${Uri.encode(Json.encodeToString(dest))}"
    }
}

composable(
    route = Route.Main.path,
    arguments = listOf(navArgument(Route.Main.ARG_DEST) {
        type = NavType.StringType; nullable = true; defaultValue = null
    }),
) { entry ->
    val dest = entry.arguments?.getString(Route.Main.ARG_DEST)
        ?.let { Json.decodeFromString<MainDestination>(it) }
        ?: MainDestination.Top
    MainScreen(
        destination = dest,           // selectedTab は dest.tab で引く
        onTabClick = onTabClick,
        snackbarHostState = snackbarHostState,
    )
}
```
// スリープ画面
SleepRoute(
    onNoticeSelected = { destination ->
        navController.navigate(Route.Main.of(destination.toMainDestination())) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = false
        }
        idleTimer.wake()
    },
)

