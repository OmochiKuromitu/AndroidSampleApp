package com.example.androidsampleapp.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidsampleapp.domain.usecase.RefreshMissedCallCountUseCase
import com.example.androidsampleapp.domain.usecase.RefreshNoticesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * 起動・タブ移動・スリープ表示のイベントを受けて、不在着信の件数と通知一覧を取り直す。
 *
 * 値を見るのは各画面の ViewModel（件数は MainViewModel と ContactViewModel、
 * 一覧は SleepViewModel と ContactViewModel）で、ここは取得を促すだけ。
 * 取得のきっかけが画面の切り替えなので、[AppNavigation] が呼ぶ。
 * 共通の取得はこの ViewModel の寿命で動く。個々のタブの非表示では中断しない。
 */
@HiltViewModel
class MissedCallViewModel @Inject constructor(
    private val refreshMissedCallCount: RefreshMissedCallCountUseCase,
    private val refreshNotices: RefreshNoticesUseCase,
) : ViewModel() {

    fun refresh() {
        // 一方の通信失敗で他方の取得を止めない。重複要求の抑止は各 Repository が行う。
        launchRefresh { refreshMissedCallCount() }
        launchRefresh { refreshNotices() }
    }

    private fun launchRefresh(request: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                request()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // 件数は前回値を残し、通知の失敗は Repository の共有状態から各画面に届く。
            }
        }
    }
}
