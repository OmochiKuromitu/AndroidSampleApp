package com.example.androidsampleapp.feature.home

import com.example.androidsampleapp.core.mvi.UiEffect
import com.example.androidsampleapp.core.mvi.UiIntent
import com.example.androidsampleapp.core.mvi.UiState
import com.example.androidsampleapp.data.Task

data class HomeState(
    val isLoading: Boolean = false,
    val tasks: List<Task> = emptyList(),
    val errorMessage: String? = null,
) : UiState

sealed interface HomeIntent : UiIntent {
    /** 画面の生成時に 1 度だけ流す。 */
    data object Started : HomeIntent
    data object ReloadClicked : HomeIntent
    data class TaskClicked(val taskId: String) : HomeIntent

    /** 非同期処理の結果も Intent として戻す。Reducer が状態変化の唯一の入口になる。 */
    data class TasksLoaded(val tasks: List<Task>) : HomeIntent
    data class LoadFailed(val message: String) : HomeIntent
}

sealed interface HomeEffect : UiEffect {
    data class OpenDetail(val taskId: String) : HomeEffect
}
