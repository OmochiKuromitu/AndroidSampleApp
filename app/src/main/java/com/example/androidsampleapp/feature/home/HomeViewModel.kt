package com.example.androidsampleapp.feature.home

import com.example.androidsampleapp.core.mvi.MviViewModel
import com.example.androidsampleapp.data.TaskRepository
import com.example.androidsampleapp.di.AppGraph

class HomeViewModel(
    private val repository: TaskRepository = AppGraph.taskRepository,
) : MviViewModel<HomeState, HomeIntent, HomeEffect>(
    initialState = HomeState(),
    reducer = HomeReducer(),
) {

    init {
        dispatch(HomeIntent.Started)
    }

    override suspend fun handle(intent: HomeIntent, previous: HomeState, current: HomeState) {
        when (intent) {
            HomeIntent.Started,
            HomeIntent.ReloadClicked,
            -> loadTasks()

            is HomeIntent.TaskClicked -> sendEffect(HomeEffect.OpenDetail(intent.taskId))

            is HomeIntent.TasksLoaded,
            is HomeIntent.LoadFailed,
            -> Unit
        }
    }

    private suspend fun loadTasks() {
        runCatching { repository.loadTasks() }
            .onSuccess { dispatch(HomeIntent.TasksLoaded(it)) }
            .onFailure { dispatch(HomeIntent.LoadFailed(it.message ?: "読み込みに失敗しました")) }
    }
}
