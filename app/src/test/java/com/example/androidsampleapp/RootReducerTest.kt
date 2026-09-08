package com.example.androidsampleapp

import org.junit.Assert.assertEquals
import org.junit.Test

class RootReducerTest {

    private val reducer = RootReducer()

    @Test
    fun `タブをタップすると選択中タブが変わる`() {
        val next = reducer.reduce(RootState(), RootIntent.TabClicked(TopLevelDestination.SEARCH))

        assertEquals(TopLevelDestination.SEARCH, next.selectedTab)
    }

    @Test
    fun `バックスタックの変化に追従する`() {
        val state = RootState(selectedTab = TopLevelDestination.PROFILE)

        val next = reducer.reduce(state, RootIntent.BackStackChanged(TopLevelDestination.HOME))

        assertEquals(TopLevelDestination.HOME, next.selectedTab)
    }
}
