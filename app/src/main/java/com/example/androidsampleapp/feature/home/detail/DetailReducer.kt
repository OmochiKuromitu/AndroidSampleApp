package com.example.androidsampleapp.feature.home.detail

import com.example.androidsampleapp.core.mvi.Reducer

class DetailReducer : Reducer<DetailState, DetailIntent> {
    override fun reduce(state: DetailState, intent: DetailIntent): DetailState = when (intent) {
        DetailIntent.Started -> state.copy(isLoading = true)
        is DetailIntent.TaskLoaded -> state.copy(isLoading = false, task = intent.task)
        DetailIntent.DoneToggled -> state.copy(isLoading = true)
        DetailIntent.BackClicked -> state
    }
}
