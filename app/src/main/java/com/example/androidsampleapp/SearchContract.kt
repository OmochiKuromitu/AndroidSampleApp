package com.example.androidsampleapp

data class SearchState(
    val query: String = "",
    val isSearching: Boolean = false,
    val results: List<Task> = emptyList(),
) : UiState {
    val showEmptyMessage: Boolean
        get() = query.isNotBlank() && !isSearching && results.isEmpty()
}

sealed interface SearchIntent : UiIntent {
    data class QueryChanged(val query: String) : SearchIntent
    data object ClearClicked : SearchIntent
    data class ResultsLoaded(val query: String, val results: List<Task>) : SearchIntent
}

sealed interface SearchEffect : UiEffect {
    data object ClearFocus : SearchEffect
}
