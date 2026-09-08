package com.example.androidsampleapp

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * サンプル用のインメモリ実装。実際の通信・DB に差し替える前提のため、
 * 呼び出し側（ViewModel）はこのクラスの型にだけ依存する。
 */
class TaskRepository {

    private val tasks = MutableStateFlow(
        listOf(
            Task("1", "MVI の骨組みを作る", "State / Intent / Reducer / Effect を用意する", isDone = true),
            Task("2", "Navigation bar を置く", "3 タブを行き来できるようにする", isDone = true),
            Task("3", "Reducer のテストを書く", "純粋関数なので JVM テストで完結する", isDone = false),
            Task("4", "Repository を差し替える", "インメモリから Retrofit / Room へ", isDone = false),
            Task("5", "DI を導入する", "手書きの AppGraph を Hilt に置き換える", isDone = false),
        )
    )

    suspend fun loadTasks(): List<Task> {
        delay(LOAD_DELAY_MS)
        return tasks.value
    }

    suspend fun findTask(id: String): Task? {
        delay(LOAD_DELAY_MS / 2)
        return tasks.value.firstOrNull { it.id == id }
    }

    suspend fun search(query: String): List<Task> {
        delay(LOAD_DELAY_MS / 2)
        if (query.isBlank()) return emptyList()
        return tasks.value.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true)
        }
    }

    suspend fun setDone(id: String, isDone: Boolean): Task? {
        delay(LOAD_DELAY_MS / 4)
        tasks.update { current ->
            current.map { if (it.id == id) it.copy(isDone = isDone) else it }
        }
        return tasks.value.firstOrNull { it.id == id }
    }

    private companion object {
        const val LOAD_DELAY_MS = 600L
    }
}
