package com.example.androidsampleapp.ui.contact

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidsampleapp.domain.model.NoticeDestination

/**
 * 連絡先画面の配線。ここが AppNavigation から呼ばれる入口になる。
 *
 * 最初に開くリストは遷移時の引数で決まる。引数は ViewModel が
 * [androidx.lifecycle.SavedStateHandle] から受け取るので、ここでは何もしない。
 * [ContactScreen] には State と操作のコールバックだけを渡し、Intent に変えるのはここで行う。
 *
 * お知らせが選ばれたら [onNoticeSelected] に流すだけで、遷移そのものは AppNavigation が行う。
 */
@Composable
fun ContactRoute(
    onNoticeSelected: (NoticeDestination) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ContactViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // LaunchedEffect の中身は最初に起動したときのまま動き続ける。親から新しいコールバックが
    // 渡されても古いほうを呼ばないよう、常に最新を指す State 越しに呼ぶ。
    val currentOnNoticeSelected by rememberUpdatedState(onNoticeSelected)

    // Effect は一回きりの出来事なので State とは別に受け取り、届いた順に 1 つずつ処理する。
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ContactEffect.NoticeSelected -> currentOnNoticeSelected(effect.destination)
            }
        }
    }

    ContactScreen(
        state = state,
        onListSelect = { viewModel.onIntent(ContactIntent.ListSelected(it)) },
        onNoticeClick = { viewModel.onIntent(ContactIntent.NoticeClicked(it)) },
        onClearNoticesClick = { viewModel.onIntent(ContactIntent.ClearNoticesClicked) },
        onClearNoticesConfirm = { viewModel.onIntent(ContactIntent.ClearNoticesConfirmed) },
        onClearNoticesDismiss = { viewModel.onIntent(ContactIntent.ClearNoticesDismissed) },
        modifier = modifier,
    )
}
