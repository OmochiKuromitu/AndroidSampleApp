package com.example.androidsampleapp

class RootReducer : Reducer<RootState, RootIntent> {
    override fun reduce(state: RootState, intent: RootIntent): RootState = when (intent) {
        is RootIntent.TabClicked -> state.copy(selectedTab = intent.destination)
        is RootIntent.BackStackChanged -> state.copy(selectedTab = intent.destination)
    }
}
