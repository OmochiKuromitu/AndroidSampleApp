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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.androidsampleapp.R
import com.example.androidsampleapp.domain.model.Aircon
import com.example.androidsampleapp.domain.model.AirconMode
import com.example.androidsampleapp.domain.model.IncomingCall
import com.example.androidsampleapp.ui.common.CenteredMessage
import com.example.androidsampleapp.ui.common.PanelButton
import com.example.androidsampleapp.ui.common.PanelPreview
import com.example.androidsampleapp.ui.common.PreviewSurface
import com.example.androidsampleapp.ui.theme.dimensions

/**
 * トップ画面の表示。State を描き、操作をコールバックで返すだけ。
 * ViewModel も Intent も Effect も知らないので、そのままプレビューできる。
 *
 * 入口は [TopRoute]。
 */
@Composable
fun TopScreen(
    state: TopState,
    onAnswerClick: () -> Unit,
    onRejectClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                onAnswer = onAnswerClick,
                onReject = onRejectClick,
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

@PanelPreview
@Composable
private fun TopScreenIdlePreview() {
    PreviewSurface {
        TopScreen(
            state = TopState(
                aircon = Aircon(
                    isOn = true,
                    mode = AirconMode.COOL,
                    targetTemperature = 26.0,
                    roomTemperature = 28.4,
                ),
            ),
            onAnswerClick = {},
            onRejectClick = {},
        )
    }
}

@PanelPreview
@Composable
private fun TopScreenIncomingCallPreview() {
    PreviewSurface {
        TopScreen(
            state = TopState(
                incomingCall = IncomingCall(roomId = "101", displayName = "玄関"),
                aircon = Aircon(isOn = false, roomTemperature = 24.1),
            ),
            onAnswerClick = {},
            onRejectClick = {},
        )
    }
}

@PanelPreview
@Composable
private fun TopScreenSendingPreview() {
    PreviewSurface {
        TopScreen(
            state = TopState(
                incomingCall = IncomingCall(roomId = "101", displayName = "玄関"),
                isSendingCommand = true,
            ),
            onAnswerClick = {},
            onRejectClick = {},
        )
    }
}
