package com.example.androidsampleapp.feature.search

import com.example.androidsampleapp.core.mvi.Reducer

class SearchReducer : Reducer<SearchState, SearchIntent> {
    override fun reduce(state: SearchState, intent: SearchIntent): SearchState = when (intent) {
        is SearchIntent.QueryChanged -> state.copy(
            query = intent.query,
            isSearching = intent.query.isNotBlank(),
            results = if (intent.query.isBlank()) emptyList() else state.results,
        )

        SearchIntent.ClearClicked -> SearchState()

        // 入力が先に進んでいたら古い結果は捨てる。
        is SearchIntent.ResultsLoaded ->
            if (intent.query == state.query) {
                state.copy(isSearching = false, results = intent.results)
            } else {
                state
            }
    }
}
