package com.example.androidsampleapp.ui.main

import com.example.androidsampleapp.core.mvi.Reducer

class MainReducer : Reducer<MainState, MainIntent> {
    override fun reduce(state: MainState, intent: MainIntent): MainState = when (intent) {
        // スリープはコンテンツタブではないので選択状態を動かさない。復帰したとき元のタブに戻る。
        is MainIntent.TabClicked ->
            if (intent.tab == MainTab.SLEEP) state else state.copy(selectedTab = intent.tab)

        is MainIntent.BackStackChanged ->
            if (intent.tab == MainTab.SLEEP) state else state.copy(selectedTab = intent.tab)

        is MainIntent.ConnectionStateChanged -> state.copy(connectionState = intent.state)

        MainIntent.IncomingCallReceived -> state.copy(selectedTab = MainTab.TOP)
    }
}
