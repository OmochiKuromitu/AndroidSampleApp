package com.example.androidsampleapp.ui.top

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * トップ画面の配線。ここが AppNavigation から呼ばれる入口になる。
 *
 * ViewModel の取得、State の購読、Effect の受け取りを担い、[TopScreen] には
 * State と操作のコールバックだけを渡す。操作を Intent に変えるのもここで、
 * Screen を表示だけに保つための層。
 */
@Composable
fun TopRoute(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: TopViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Effect は一回きりの出来事なので State とは別に受け取り、届いた順に 1 つずつ処理する。
    // 画面が裏に回っている間も受け取る（前面に戻るまで溜めることはしない）。
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is TopEffect.ShowMessage ->
                    snackbarHostState.showSnackbar(context.getString(effect.messageRes))
            }
        }
    }

    TopScreen(
        state = state,
        onAnswerClick = { viewModel.onIntent(TopIntent.AnswerClicked) },
        onRejectClick = { viewModel.onIntent(TopIntent.RejectClicked) },
        modifier = modifier,
    )
}
