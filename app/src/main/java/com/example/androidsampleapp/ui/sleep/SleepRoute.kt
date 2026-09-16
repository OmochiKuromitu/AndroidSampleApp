package com.example.androidsampleapp.ui.sleep

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidsampleapp.domain.model.NoticeDestination

/**
 * スリープ画面の配線。ここが AppNavigation から呼ばれる入口になる。
 *
 * Effect を受けて [onUnlock] / [onNoticeSelected] に流すだけで、遷移そのものは
 * AppNavigation が行う。[SleepScreen] は State を描くだけに保ち、操作を Intent に変えるのはここ。
 */
@Composable
fun SleepRoute(
    onUnlock: () -> Unit,
    onNoticeSelected: (NoticeDestination) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SleepViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // LaunchedEffect の中身は最初に起動したときのまま動き続ける。親から新しいコールバックが
    // 渡されても古いほうを呼ばないよう、常に最新を指す State 越しに呼ぶ。
    val currentOnUnlock by rememberUpdatedState(onUnlock)
    val currentOnNoticeSelected by rememberUpdatedState(onNoticeSelected)

    // Effect は一回きりの出来事なので State とは別に受け取り、届いた順に 1 つずつ処理する。
    // 画面が裏に回っている間も受け取る（前面に戻るまで溜めることはしない）。
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                SleepEffect.Unlocked -> currentOnUnlock()
                is SleepEffect.NoticeSelected -> currentOnNoticeSelected(effect.destination)
            }
        }
    }

    SleepScreen(
        state = state,
        onNoticeClick = { viewModel.onIntent(SleepIntent.NoticeClicked(it)) },
        onClearNoticesClick = { viewModel.onIntent(SleepIntent.ClearNoticesClicked) },
        onClearNoticesConfirm = { viewModel.onIntent(SleepIntent.ClearNoticesConfirmed) },
        onClearNoticesDismiss = { viewModel.onIntent(SleepIntent.ClearNoticesDismissed) },
        onUnlockDrag = { viewModel.onIntent(SleepIntent.UnlockDragged(it)) },
        onUnlockCancel = { viewModel.onIntent(SleepIntent.UnlockCancelled) },
        modifier = modifier,
    )
}
