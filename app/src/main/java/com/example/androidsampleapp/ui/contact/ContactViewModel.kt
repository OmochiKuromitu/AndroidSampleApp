package com.example.androidsampleapp.ui.contact

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.androidsampleapp.core.MissedCallManager
import com.example.androidsampleapp.core.mvi.MviViewModel
import com.example.androidsampleapp.domain.usecase.GetCallHistoriesUseCase
import com.example.androidsampleapp.domain.usecase.GetContactsUseCase
import com.example.androidsampleapp.ui.common.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class ContactViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    missedCallManager: MissedCallManager,
    private val getContacts: GetContactsUseCase,
    private val getCallHistories: GetCallHistoriesUseCase,
) : MviViewModel<ContactState, ContactIntent, ContactEffect>(
    initialState = ContactState(
        // 遷移時に渡された引数で最初のリストを決める。
        // 初期状態なので Intent ではなくここで解決する。Intent にすると、
        // 取得が終わる前に一瞬だけ電話帳が見えてから履歴へ切り替わる。
        selectedList = ContactList.fromArgument(savedStateHandle[Route.ARG_CONTACT_LIST]),
    ),
    reducer = ContactReducer(),
) {

    init {
        // 取得は MissedCallManager が行う。ここは件数を見るだけ。
        viewModelScope.launch {
            missedCallManager.missedCallCount.collect {
                dispatch(ContactIntent.MissedCallCountChanged(it))
            }
        }
        dispatch(ContactIntent.Started)
    }

    override suspend fun handle(intent: ContactIntent, previous: ContactState, current: ContactState) {
        when (intent) {
            // 両方まとめて取る。リストの切り替えは表示の出し分けだけにして、
            // タブを触るたびに通信が走らないようにする。
            ContactIntent.Started -> load()

            is ContactIntent.ListSelected,
            is ContactIntent.Loaded,
            ContactIntent.LoadFailed,
            is ContactIntent.MissedCallCountChanged,
            -> Unit
        }
    }

    private suspend fun load() {
        runCatching { getContacts() to getCallHistories() }
            .onSuccess { (contacts, histories) ->
                dispatch(ContactIntent.Loaded(contacts = contacts, histories = histories))
            }
            .onFailure { dispatch(ContactIntent.LoadFailed) }
    }
}
