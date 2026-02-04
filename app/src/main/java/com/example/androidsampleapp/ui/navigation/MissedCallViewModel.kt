package com.example.androidsampleapp.ui.navigation

import androidx.lifecycle.ViewModel
import com.example.androidsampleapp.core.MissedCallManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * 不在着信の件数と通知一覧の取得を Composable から促すための窓口。中身は [MissedCallManager] が持つ。
 *
 * 値を見るのは各画面の ViewModel（件数は MainViewModel と ContactViewModel、
 * 一覧は SleepViewModel と ContactViewModel）で、ここは取得を促すだけ。
 * 取得のきっかけが画面の切り替えなので、[AppNavigation] が呼ぶ。
 */
@HiltViewModel
class MissedCallViewModel @Inject constructor(
    private val missedCallManager: MissedCallManager,
) : ViewModel() {

    fun refresh() = missedCallManager.refresh()
}
