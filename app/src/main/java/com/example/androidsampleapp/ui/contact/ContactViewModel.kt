package com.example.androidsampleapp.ui.contact

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidsampleapp.domain.usecase.ClearNoticesUseCase
import com.example.androidsampleapp.domain.usecase.MarkMissedCallsAsReadUseCase
import com.example.androidsampleapp.domain.usecase.ObserveAddressBookUseCase
import com.example.androidsampleapp.domain.usecase.ObserveMissedCallCountUseCase
import com.example.androidsampleapp.domain.usecase.ObserveNoticesUseCase
import com.example.androidsampleapp.domain.usecase.RefreshAddressBookUseCase
import com.example.androidsampleapp.ui.common.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * 連絡先画面の ViewModel。
 *
 * - 最初に開くリストは遷移の引数（SavedStateHandle）から、初期状態の時点で決める。
 * - 電話帳と履歴は ContactRepository を購読し、値が変わるたびに Intent にする。
 *   画面を開いたら取り直しを頼み、失敗したときだけ Intent で戻す（成功した結果は購読側に流れる）。
 * - 不在着信の件数と通知一覧も UseCase を通して購読する。共通の取得のきっかけは AppNavigation。
 * - 履歴を見せたら既読の UseCase、確認ダイアログで消去を選ばれたら消去の UseCase を呼ぶ。
 *   お知らせがタップされたら Effect を出し、どこへ行くかは AppNavigation が決める。
 */
@HiltViewModel
class ContactViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeAddressBook: ObserveAddressBookUseCase,
    observeMissedCallCount: ObserveMissedCallCountUseCase,
    observeNotices: ObserveNoticesUseCase,
    private val refreshAddressBook: RefreshAddressBookUseCase,
    private val markMissedCallsAsRead: MarkMissedCallsAsReadUseCase,
    private val clearNotices: ClearNoticesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ContactState(
            // 遷移時に渡された引数で最初のリストを決める。
            // 初期状態なので Intent ではなくここで解決する。Intent にすると、
            // 取得が終わる前に一瞬だけ電話帳が見えてから履歴へ切り替わる。
            selectedList = ContactList.fromArgument(savedStateHandle[Route.ARG_CONTACT_LIST]),
        ),
    )
    val uiState: StateFlow<ContactState> = _uiState.asStateFlow()

    private val _effect = Channel<ContactEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private val reducer = ContactReducer()

    init {
        // まだ一度も取れていない間は null が流れる。読み込み中の表示は State が決めるので、ここでは捨てる。
        viewModelScope.launch {
            observeAddressBook().filterNotNull().collect {
                onIntent(ContactIntent.AddressBookChanged(it))
            }
        }
        // 前回の共有値も購読を始めた時点で届く。
        viewModelScope.launch {
            observeMissedCallCount().collect {
                onIntent(ContactIntent.MissedCallCountChanged(it))
            }
        }
        // 取得・消去の結果と失敗状況は Repository の共有状態として届く。
        viewModelScope.launch {
            observeNotices().collect { onIntent(ContactIntent.NoticesChanged(it)) }
        }
        onIntent(ContactIntent.Started)
    }

    /**
     * 状態を変えうる入力の入口。Route からの操作も、購読した値の変化も、通信の結果も、すべてここを通す。
     *
     * Reducer で次の状態を作って [_uiState] に入れ、そのあと [handle] で副作用を実行する。
     * 副作用は並行に走らせる。長い通信が、後から来た Intent の反映を止めないようにするため。
     */
    fun onIntent(intent: ContactIntent) {
        var previous: ContactState
        var current: ContactState
        // 読んでから書くまでの間に別の更新が入っていたら、読み直してやり直す。
        do {
            previous = _uiState.value
            current = reducer.reduce(previous, intent)
        } while (!_uiState.compareAndSet(previous, current))
        viewModelScope.launch { handle(intent, previous, current) }
    }

    private suspend fun handle(intent: ContactIntent, previous: ContactState, current: ContactState) {
        when (intent) {
            // 両方まとめて取り直す。リストの切り替えは表示の出し分けだけにして、
            // タブを触るたびに通信が走らないようにする。
            ContactIntent.Started -> {
                refresh()
                // 履歴から開いたなら、その時点で見せたことになる。
                if (current.selectedList == ContactList.HISTORY) {
                    requestSharedState { markMissedCallsAsRead() }
                }
            }

            // 履歴を見せたので既読にする。件数で間引かないのは、件数がまだ届いていない
            // タイミングで開かれると取りこぼすため。既読 API は何度呼んでも同じ結果になる前提。
            is ContactIntent.ListSelected ->
                if (intent.list == ContactList.HISTORY) {
                    requestSharedState { markMissedCallsAsRead() }
                }

            // 結果は購読側から NoticesChanged で戻る。
            // 押しただけの ClearNoticesClicked では消さない（確認ダイアログを出すだけ）。
            ContactIntent.ClearNoticesConfirmed -> requestSharedState { clearNotices() }

            is ContactIntent.NoticeClicked ->
                _effect.send(ContactEffect.NoticeSelected(intent.notice.destination))

            is ContactIntent.AddressBookChanged,
            ContactIntent.LoadFailed,
            is ContactIntent.MissedCallCountChanged,
            is ContactIntent.NoticesChanged,
            ContactIntent.ClearNoticesClicked,
            ContactIntent.ClearNoticesDismissed,
            -> Unit
        }
    }

    /** 成功した結果は ContactRepository の値の変化として購読側に届くので、ここは失敗だけを戻す。 */
    private suspend fun refresh() {
        try {
            refreshAddressBook()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            onIntent(ContactIntent.LoadFailed)
        }
    }

    private suspend fun requestSharedState(request: suspend () -> Unit) {
        try {
            request()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // 件数は前回値を残す。通知は失敗したら空の一覧が NoticesChanged で届く。
        }
    }
}
