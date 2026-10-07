package com.example.androidsampleapp.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidsampleapp.domain.usecase.RefreshMissedCallCountUseCase
import com.example.androidsampleapp.domain.usecase.RefreshNoticesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch

/**
 * 起動・タブ移動・スリープ表示のイベントを受けて、不在着信の件数と通知一覧を取り直す。
 *
 * 値を見るのは各画面の ViewModel（件数は MainViewModel と ContactViewModel、
 * 一覧は SleepViewModel と ContactViewModel）で、ここは取得を促すだけ。
 * 取得のきっかけが画面の切り替えなので、[AppNavigation] が呼ぶ。
 * 共通の取得はこの ViewModel の寿命で動く。個々のタブの非表示では中断しない。
 *
 * タブ移動が続いたときは、最後の切り替えで投げた要求を優先する。種類（件数 / 通知）ごとに
 * 実行中の要求を打ち切ってから投げ直すので、遅れて届いた古い結果で上書きされない。
 */
@HiltViewModel
class MissedCallViewModel @Inject constructor(
    private val refreshMissedCallCount: RefreshMissedCallCountUseCase,
    private val refreshNotices: RefreshNoticesUseCase,
) : ViewModel() {

    private var missedCallCountJob: Job? = null
    private var noticesJob: Job? = null

    fun refresh() {
        // 一方の通信失敗で他方の取得を止めない。打ち切るのも同じ種類の要求だけ。
        missedCallCountJob = relaunch(missedCallCountJob) { refreshMissedCallCount() }
        noticesJob = relaunch(noticesJob) { refreshNotices() }
    }

    private fun relaunch(previous: Job?, request: suspend () -> Unit): Job =
        viewModelScope.launch {
            // 実行中の前の要求をキャンセルし、新しい要求を優先する。
            // キャンセル後に終わるまで待つのは、前の要求が Repository のロックを外す前に投げると、
            // 実行中とみなされて新しい方まで間引かれ、どちらも取得しなくなるため。
            previous?.cancelAndJoin()
            try {
                request()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // 件数は前回値を残し、通知は Repository が一覧を空にして各画面に届ける。
            }
        }
}
