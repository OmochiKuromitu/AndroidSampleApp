package com.example.androidsampleapp

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

class DetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: TaskRepository = AppGraph.taskRepository,
) : MviViewModel<DetailState, DetailIntent, DetailEffect>(
    initialState = DetailState(
        taskId = checkNotNull(savedStateHandle.get<String>(HomeRoutes.ARG_TASK_ID)) {
            "${HomeRoutes.ARG_TASK_ID} が渡されていません"
        },
    ),
    reducer = DetailReducer(),
) {

    init {
        dispatch(DetailIntent.Started)
    }

    override suspend fun handle(intent: DetailIntent, previous: DetailState, current: DetailState) {
        when (intent) {
            DetailIntent.Started ->
                dispatch(DetailIntent.TaskLoaded(repository.findTask(current.taskId)))

            DetailIntent.DoneToggled -> {
                val target = previous.task ?: return
                val updated = repository.setDone(target.id, !target.isDone)
                dispatch(DetailIntent.TaskLoaded(updated))
                sendEffect(
                    DetailEffect.ShowMessage(
                        if (updated?.isDone == true) "完了にしました" else "未完了に戻しました",
                    ),
                )
            }

            DetailIntent.BackClicked -> sendEffect(DetailEffect.NavigateBack)

            is DetailIntent.TaskLoaded -> Unit
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { DetailViewModel(savedStateHandle = createSavedStateHandle()) }
        }
    }
}
