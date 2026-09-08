package com.example.androidsampleapp

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
