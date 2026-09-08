package com.example.androidsampleapp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is HomeEffect.OpenDetail -> onOpenDetail(effect.taskId)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.home_title)) },
            actions = {
                IconButton(onClick = { viewModel.dispatch(HomeIntent.ReloadClicked) }) {
                    Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.home_reload))
                }
            },
        )
        HomeContent(
            state = state,
            onTaskClick = { viewModel.dispatch(HomeIntent.TaskClicked(it)) },
            onRetry = { viewModel.dispatch(HomeIntent.ReloadClicked) },
            listState = listState,
        )
    }
}

@Composable
private fun HomeContent(
    state: HomeState,
    onTaskClick: (String) -> Unit,
    onRetry: () -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState,
) {
    val errorMessage = state.errorMessage
    when {
        state.isLoading && state.tasks.isEmpty() -> CenterBox { CircularProgressIndicator() }

        errorMessage != null -> CenterBox {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(errorMessage, style = MaterialTheme.typography.bodyLarge)
                TextButton(onClick = onRetry) { Text(stringResource(R.string.home_reload)) }
            }
        }

        state.tasks.isEmpty() -> CenterBox { Text(stringResource(R.string.home_empty)) }

        else -> LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            items(state.tasks, key = { it.id }) { task ->
                TaskRow(task = task, onClick = { onTaskClick(task.id) })
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun TaskRow(task: Task, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(task.title) },
        supportingContent = { Text(task.description) },
        trailingContent = {
            if (task.isDone) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun CenterBox(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}
