package com.example.androidsampleapp.ui.contact

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidsampleapp.core.mvi.CollectEffect
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
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is ContactEffect.NoticeSelected -> onNoticeSelected(effect.destination)
        }
    }

    ContactScreen(
        state = state,
        onListSelect = { viewModel.dispatch(ContactIntent.ListSelected(it)) },
        onNoticeClick = { viewModel.dispatch(ContactIntent.NoticeClicked(it)) },
        onClearNoticesClick = { viewModel.dispatch(ContactIntent.ClearNoticesClicked) },
        modifier = modifier,
    )
}
