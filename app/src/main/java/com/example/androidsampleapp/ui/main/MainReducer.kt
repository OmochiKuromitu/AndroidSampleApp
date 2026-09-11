package com.example.androidsampleapp.ui.main

import com.example.androidsampleapp.core.mvi.Reducer

class MainReducer : Reducer<MainState, MainIntent> {
    override fun reduce(state: MainState, intent: MainIntent): MainState = when (intent) {
        is MainIntent.ConnectionStateChanged -> state.copy(connectionState = intent.state)
    }
}
