package com.example.androidsampleapp.ui.groupcall

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * グループ通話の画面の配線。ここが AppNavigation から呼ばれる入口になる。
 *
 * Effect を受けて [onCallEnded] に流すだけで、画面を閉じるのは AppNavigation。
 * [GroupCallScreen] は State を描くだけに保ち、操作を Intent に変えるのはここ。
 */
@Composable
fun GroupCallRoute(
    onCallEnded: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GroupCallViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // LaunchedEffect の中身は最初に起動したときのまま動き続けるので、常に最新のコールバックを呼ぶ。
    val currentOnCallEnded by rememberUpdatedState(onCallEnded)

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                GroupCallEffect.CallEnded -> currentOnCallEnded()
            }
        }
    }

    GroupCallScreen(
        state = state,
        onMicVolumeChange = { viewModel.onIntent(GroupCallIntent.MicVolumeChanged(it)) },
        onSpeakerClick = { viewModel.onIntent(GroupCallIntent.SpeakerToggled) },
        onMuteClick = { viewModel.onIntent(GroupCallIntent.MuteToggled) },
        onEndCallClick = { viewModel.onIntent(GroupCallIntent.EndCallClicked) },
        modifier = modifier,
    )
}
