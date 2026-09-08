package com.example.androidsampleapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchReducerTest {

    private val reducer = SearchReducer()

    @Test
    fun `入力があれば検索中になる`() {
        val next = reducer.reduce(SearchState(), SearchIntent.QueryChanged("mvi"))

        assertEquals("mvi", next.query)
        assertTrue(next.isSearching)
    }

    @Test
    fun `空入力なら結果を捨てる`() {
        val state = SearchState(query = "mvi", results = listOf(Task("1", "a", "b", false)))

        val next = reducer.reduce(state, SearchIntent.QueryChanged(""))

        assertFalse(next.isSearching)
        assertTrue(next.results.isEmpty())
    }

    @Test
    fun `古いクエリの結果は無視する`() {
        val state = SearchState(query = "mvi", isSearching = true)
        val stale = listOf(Task("1", "a", "b", false))

        val next = reducer.reduce(state, SearchIntent.ResultsLoaded("mv", stale))

        assertEquals(state, next)
    }

    @Test
    fun `現在のクエリの結果は反映する`() {
        val state = SearchState(query = "mvi", isSearching = true)
        val results = listOf(Task("1", "a", "b", false))

        val next = reducer.reduce(state, SearchIntent.ResultsLoaded("mvi", results))

        assertFalse(next.isSearching)
        assertEquals(results, next.results)
    }
}
