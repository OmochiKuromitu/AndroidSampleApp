package com.example.androidsampleapp.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
fun ProfileScreen(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is ProfileEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(title = { Text(stringResource(R.string.profile_title)) })

        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(state.userName, style = MaterialTheme.typography.headlineSmall)
            Text(state.email, style = MaterialTheme.typography.bodyMedium)

            SettingRow(
                label = stringResource(R.string.profile_notifications),
                checked = state.notificationsEnabled,
                onCheckedChange = { viewModel.dispatch(ProfileIntent.NotificationsToggled(it)) },
            )
            SettingRow(
                label = stringResource(R.string.profile_dark_theme),
                checked = state.darkThemeEnabled,
                onCheckedChange = { viewModel.dispatch(ProfileIntent.DarkThemeToggled(it)) },
            )

            Button(
                onClick = { viewModel.dispatch(ProfileIntent.SaveClicked) },
                enabled = !state.isSaving,
            ) {
                Text(stringResource(R.string.profile_save))
            }
        }
    }
}

@Composable
private fun SettingRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
