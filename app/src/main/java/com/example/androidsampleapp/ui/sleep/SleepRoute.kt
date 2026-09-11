package com.example.androidsampleapp.ui.sleep

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidsampleapp.core.mvi.CollectEffect
import com.example.androidsampleapp.domain.model.NoticeDestination

/**
 * スリープ画面の配線。ここが AppNavigation から呼ばれる入口になる。
 *
 * Effect を受けて [onUnlock] / [onNoticeSelected] に流すだけで、遷移そのものは
 * AppNavigation が行う。[SleepScreen] は State を描くだけに保つ。
 */
@Composable
fun SleepRoute(
    onUnlock: () -> Unit,
    onNoticeSelected: (NoticeDestination) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SleepViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            SleepEffect.Unlocked -> onUnlock()
            is SleepEffect.NoticeSelected -> onNoticeSelected(effect.destination)
        }
    }

    SleepScreen(state = state, onIntent = viewModel::dispatch, modifier = modifier)
}
