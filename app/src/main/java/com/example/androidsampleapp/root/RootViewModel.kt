package com.example.androidsampleapp.root

import com.example.androidsampleapp.core.mvi.MviViewModel

class RootViewModel : MviViewModel<RootState, RootIntent, RootEffect>(
    initialState = RootState(),
    reducer = RootReducer(),
) {
    override suspend fun handle(intent: RootIntent, previous: RootState, current: RootState) {
        when (intent) {
            is RootIntent.TabClicked -> {
                // 同じタブの再タップは「そのタブの先頭まで戻す」。State は変わらない動作なので Effect。
                if (previous.selectedTab == intent.destination) {
                    sendEffect(RootEffect.PopToTabRoot(intent.destination))
                } else {
                    sendEffect(RootEffect.NavigateToTab(intent.destination))
                }
            }

            // NavController が先に動いた結果の同期なので、ここから再遷移はしない（ループ防止）。
            is RootIntent.BackStackChanged -> Unit
        }
    }
}
