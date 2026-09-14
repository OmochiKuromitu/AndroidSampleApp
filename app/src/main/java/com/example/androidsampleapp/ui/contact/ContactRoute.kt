package com.example.androidsampleapp.ui.contact

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * 連絡先画面の配線。ここが AppNavigation から呼ばれる入口になる。
 *
 * 最初に開くリストは遷移時の引数で決まる。引数は ViewModel が
 * [androidx.lifecycle.SavedStateHandle] から受け取るので、ここでは何もしない。
 * [ContactScreen] には State と操作のコールバックだけを渡し、Intent に変えるのはここで行う。
 */
@Composable
fun ContactRoute(
    modifier: Modifier = Modifier,
    viewModel: ContactViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ContactScreen(
        state = state,
        onListSelect = { viewModel.onIntent(ContactIntent.ListSelected(it)) },
        modifier = modifier,
    )
}
