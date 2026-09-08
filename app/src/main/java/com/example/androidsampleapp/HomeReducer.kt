package com.example.androidsampleapp

class HomeReducer : Reducer<HomeState, HomeIntent> {
    override fun reduce(state: HomeState, intent: HomeIntent): HomeState = when (intent) {
        HomeIntent.Started,
        HomeIntent.ReloadClicked,
        -> state.copy(isLoading = true, errorMessage = null)

        is HomeIntent.TasksLoaded -> state.copy(
            isLoading = false,
            tasks = intent.tasks,
            errorMessage = null,
        )

        is HomeIntent.LoadFailed -> state.copy(
            isLoading = false,
            errorMessage = intent.message,
        )

        // 遷移は状態を変えない。Effect 側で扱う。
        is HomeIntent.TaskClicked -> state
    }
}
