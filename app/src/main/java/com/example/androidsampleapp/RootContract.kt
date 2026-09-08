package com.example.androidsampleapp

/**
 * 選択中のタブは「状態」なので State に持つ。Navigation bar のハイライトはここだけを見る。
 */
data class RootState(
    val selectedTab: TopLevelDestination = TopLevelDestination.HOME,
) : UiState

sealed interface RootIntent : UiIntent {
    /** タブがタップされた。 */
    data class TabClicked(val destination: TopLevelDestination) : RootIntent

    /** 戻るキーなど、NavController 側の都合で現在地が変わった。State を追従させるだけ。 */
    data class BackStackChanged(val destination: TopLevelDestination) : RootIntent
}

/**
 * 遷移そのものは「一回きりの命令」であって状態ではないので Effect にする。
 * State に持たせると再生成のたびに再遷移してしまう。
 */
sealed interface RootEffect : UiEffect {
    data class NavigateToTab(val destination: TopLevelDestination) : RootEffect
    data class PopToTabRoot(val destination: TopLevelDestination) : RootEffect
}
