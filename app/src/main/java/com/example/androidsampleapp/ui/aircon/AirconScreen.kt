package com.example.androidsampleapp.ui.aircon

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidsampleapp.R
import com.example.androidsampleapp.core.mvi.CollectEffect
import com.example.androidsampleapp.domain.model.Aircon
import com.example.androidsampleapp.domain.model.AirconMode
import com.example.androidsampleapp.domain.model.ConnectionState
import com.example.androidsampleapp.ui.common.PanelPreview
import com.example.androidsampleapp.ui.common.PreviewSurface
import com.example.androidsampleapp.ui.theme.dimensions

/**
 * ViewModel と Effect の受け口。描画は [AirconContent] が担う。
 */
@Composable
fun AirconScreen(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: AirconViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is AirconEffect.ShowMessage ->
                snackbarHostState.showSnackbar(context.getString(effect.messageRes))
        }
    }

    AirconContent(state = state, onIntent = viewModel::dispatch, modifier = modifier)
}

@Composable
private fun AirconContent(
    state: AirconState,
    onIntent: (AirconIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(MaterialTheme.dimensions.spaceLarge),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spaceLarge),
    ) {
        if (!state.connectionState.isConnected) {
            Text(
                text = stringResource(R.string.aircon_offline),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.aircon_power), style = MaterialTheme.typography.titleLarge)
            Switch(
                checked = state.aircon.isOn,
                onCheckedChange = { onIntent(AirconIntent.PowerToggled(it)) },
                enabled = state.isOperable,
            )
        }

        Text(
            text = stringResource(R.string.aircon_room_temperature, state.aircon.roomTemperature),
            style = MaterialTheme.typography.bodyLarge,
        )

        TemperatureControl(
            targetTemperature = state.aircon.targetTemperature,
            enabled = state.isOperable && state.aircon.isOn,
            onDown = { onIntent(AirconIntent.TemperatureDownClicked) },
            onUp = { onIntent(AirconIntent.TemperatureUpClicked) },
        )

        Text(stringResource(R.string.aircon_mode), style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spaceSmall)) {
            AirconMode.entries.forEach { mode ->
                FilterChip(
                    selected = mode == state.aircon.mode,
                    onClick = { onIntent(AirconIntent.ModeSelected(mode)) },
                    enabled = state.isOperable && state.aircon.isOn,
                    label = { Text(mode.label) },
                )
            }
        }
    }
}

@Composable
private fun TemperatureControl(
    targetTemperature: Double,
    enabled: Boolean,
    onDown: () -> Unit,
    onUp: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledIconButton(
            onClick = onDown,
            enabled = enabled,
            modifier = Modifier.size(MaterialTheme.dimensions.minTouchTarget),
        ) {
            Icon(
                imageVector = Icons.Filled.Remove,
                contentDescription = stringResource(R.string.aircon_temperature_down),
            )
        }

        Text(
            text = stringResource(R.string.aircon_target_temperature, targetTemperature),
            style = MaterialTheme.typography.headlineMedium,
        )

        FilledIconButton(
            onClick = onUp,
            enabled = enabled,
            modifier = Modifier.size(MaterialTheme.dimensions.minTouchTarget),
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(R.string.aircon_temperature_up),
            )
        }
    }
}

@PanelPreview
@Composable
private fun AirconContentRunningPreview() {
    PreviewSurface {
        AirconContent(
            state = AirconState(
                aircon = Aircon(
                    isOn = true,
                    mode = AirconMode.COOL,
                    targetTemperature = 26.0,
                    roomTemperature = 28.4,
                ),
                connectionState = ConnectionState.CONNECTED,
            ),
            onIntent = {},
        )
    }
}

@PanelPreview
@Composable
private fun AirconContentStoppedPreview() {
    PreviewSurface {
        AirconContent(
            state = AirconState(
                aircon = Aircon(isOn = false, roomTemperature = 22.0),
                connectionState = ConnectionState.CONNECTED,
            ),
            onIntent = {},
        )
    }
}

@PanelPreview
@Composable
private fun AirconContentOfflinePreview() {
    PreviewSurface {
        AirconContent(
            state = AirconState(connectionState = ConnectionState.DISCONNECTED),
            onIntent = {},
        )
    }
}
