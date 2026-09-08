package com.example.androidsampleapp.root

import com.example.androidsampleapp.core.mvi.Reducer

class RootReducer : Reducer<RootState, RootIntent> {
    override fun reduce(state: RootState, intent: RootIntent): RootState = when (intent) {
        is RootIntent.TabClicked -> state.copy(selectedTab = intent.destination)
        is RootIntent.BackStackChanged -> state.copy(selectedTab = intent.destination)
    }
}
