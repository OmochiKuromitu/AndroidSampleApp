package com.example.androidsampleapp.ui.sleep

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidsampleapp.R
import com.example.androidsampleapp.core.mvi.CollectEffect

/**
 * 無操作が続いたときに出す全画面表示。下部バーは出さない。
 */
@Composable
fun SleepScreen(
    onWake: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SleepViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val interactionSource = remember { MutableInteractionSource() }

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            SleepEffect.Wake -> onWake()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { viewModel.dispatch(SleepIntent.ScreenTapped) },
            ),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = state.timeText,
            style = MaterialTheme.typography.displayLarge,
            color = Color.White,
        )
        Text(
            text = state.dateText,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White.copy(alpha = 0.7f),
        )
        Text(
            text = stringResource(R.string.sleep_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.4f),
        )
    }
}
