package com.example.androidsampleapp.feature.home

import com.example.androidsampleapp.data.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeReducerTest {

    private val reducer = HomeReducer()

    @Test
    fun `Started で読み込み中になる`() {
        val next = reducer.reduce(HomeState(errorMessage = "前回の失敗"), HomeIntent.Started)

        assertTrue(next.isLoading)
        assertNull(next.errorMessage)
    }

    @Test
    fun `TasksLoaded で一覧が入り読み込みが終わる`() {
        val tasks = listOf(Task("1", "タイトル", "説明", isDone = false))

        val next = reducer.reduce(HomeState(isLoading = true), HomeIntent.TasksLoaded(tasks))

        assertFalse(next.isLoading)
        assertEquals(tasks, next.tasks)
    }

    @Test
    fun `LoadFailed でエラーが入る`() {
        val next = reducer.reduce(HomeState(isLoading = true), HomeIntent.LoadFailed("圏外です"))

        assertFalse(next.isLoading)
        assertEquals("圏外です", next.errorMessage)
    }

    @Test
    fun `TaskClicked は状態を変えない`() {
        val state = HomeState(tasks = listOf(Task("1", "タイトル", "説明", isDone = false)))

        val next = reducer.reduce(state, HomeIntent.TaskClicked("1"))

        assertEquals(state, next)
    }
}
