package com.example.androidsampleapp.feature.home.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.androidsampleapp.R
import com.example.androidsampleapp.core.mvi.CollectEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: DetailViewModel = viewModel(factory = DetailViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            DetailEffect.NavigateBack -> onBack()
            is DetailEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.home_detail_title)) },
            navigationIcon = {
                IconButton(onClick = { viewModel.dispatch(DetailIntent.BackClicked) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.home_detail_back),
                    )
                }
            },
        )

        val task = state.task
        when {
            state.isLoading && task == null -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            task == null -> Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center,
            ) { Text(stringResource(R.string.home_empty)) }

            else -> Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(task.title, style = MaterialTheme.typography.headlineSmall)
                Text(task.description, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = if (task.isDone) "状態: 完了" else "状態: 未完了",
                    style = MaterialTheme.typography.labelLarge,
                )
                Button(
                    onClick = { viewModel.dispatch(DetailIntent.DoneToggled) },
                    enabled = !state.isLoading,
                ) {
                    Text(stringResource(R.string.home_detail_toggle_done))
                }
            }
        }
    }
}
