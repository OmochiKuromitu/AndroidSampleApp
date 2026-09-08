package com.example.androidsampleapp

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val repository: TaskRepository = AppGraph.taskRepository,
) : MviViewModel<SearchState, SearchIntent, SearchEffect>(
    initialState = SearchState(),
    reducer = SearchReducer(),
) {

    init {
        viewModelScope.launch {
            // 入力のたびに検索を投げず、state から派生させた query を間引いて実行する。
            state.map { it.query }
                .distinctUntilChanged()
                .debounce(DEBOUNCE_MS)
                .collectLatest { query ->
                    val results = if (query.isBlank()) emptyList() else repository.search(query)
                    dispatch(SearchIntent.ResultsLoaded(query, results))
                }
        }
    }

    override suspend fun handle(intent: SearchIntent, previous: SearchState, current: SearchState) {
        when (intent) {
            SearchIntent.ClearClicked -> sendEffect(SearchEffect.ClearFocus)
            is SearchIntent.QueryChanged,
            is SearchIntent.ResultsLoaded,
            -> Unit
        }
    }

    private companion object {
        const val DEBOUNCE_MS = 300L
    }
}
