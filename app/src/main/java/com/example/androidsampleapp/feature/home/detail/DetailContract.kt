package com.example.androidsampleapp.feature.home.detail

import com.example.androidsampleapp.core.mvi.UiEffect
import com.example.androidsampleapp.core.mvi.UiIntent
import com.example.androidsampleapp.core.mvi.UiState
import com.example.androidsampleapp.data.Task

data class DetailState(
    val taskId: String = "",
    val isLoading: Boolean = true,
    val task: Task? = null,
) : UiState

sealed interface DetailIntent : UiIntent {
    data object Started : DetailIntent
    data object DoneToggled : DetailIntent
    data object BackClicked : DetailIntent
    data class TaskLoaded(val task: Task?) : DetailIntent
}

sealed interface DetailEffect : UiEffect {
    data object NavigateBack : DetailEffect
    data class ShowMessage(val message: String) : DetailEffect
}
