package com.example.androidsampleapp.ui.top

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidsampleapp.R
import com.example.androidsampleapp.core.mvi.CollectEffect
import com.example.androidsampleapp.domain.model.Aircon
import com.example.androidsampleapp.domain.model.IncomingCall
import com.example.androidsampleapp.ui.common.CenteredMessage
import com.example.androidsampleapp.ui.common.PanelButton
import com.example.androidsampleapp.ui.theme.dimensions

@Composable
fun TopScreen(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: TopViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is TopEffect.ShowMessage ->
                snackbarHostState.showSnackbar(context.getString(effect.messageRes))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(MaterialTheme.dimensions.spaceLarge),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spaceLarge),
    ) {
        val call = state.incomingCall
        if (call != null) {
            IncomingCallCard(
                call = call,
                enabled = !state.isSendingCommand,
                onAnswer = { viewModel.dispatch(TopIntent.AnswerClicked) },
                onReject = { viewModel.dispatch(TopIntent.RejectClicked) },
            )
        }

        AirconSummaryCard(aircon = state.aircon)

        if (call == null) {
            CenteredMessage(
                message = stringResource(R.string.top_idle),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun IncomingCallCard(
    call: IncomingCall,
    enabled: Boolean,
    onAnswer: () -> Unit,
    onReject: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.dimensions.spaceLarge),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spaceMedium),
        ) {
            Text(
                text = stringResource(R.string.top_incoming_call, call.displayName),
                style = MaterialTheme.typography.headlineMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spaceMedium)) {
                PanelButton(
                    text = stringResource(R.string.top_answer),
                    onClick = onAnswer,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                )
                PanelButton(
                    text = stringResource(R.string.top_reject),
                    onClick = onReject,
                    enabled = enabled,
                    containerColor = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun AirconSummaryCard(aircon: Aircon) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(MaterialTheme.dimensions.spaceLarge),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spaceSmall),
        ) {
            Text(
                text = stringResource(R.string.top_aircon_summary),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(R.string.aircon_room_temperature, aircon.roomTemperature),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = if (aircon.isOn) {
                    stringResource(R.string.aircon_target_temperature, aircon.targetTemperature) +
                        "（${aircon.mode.label}）"
                } else {
                    stringResource(R.string.aircon_off)
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
