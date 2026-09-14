package com.example.androidsampleapp.ui.navigation

import androidx.lifecycle.ViewModel
import com.example.androidsampleapp.core.NoticeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * お知らせの取得を Composable から促すための窓口。中身は [NoticeManager] が持つ。
 *
 * 一覧を見るのは各画面の ViewModel（[com.example.androidsampleapp.ui.sleep.SleepViewModel] と
 * [com.example.androidsampleapp.ui.contact.ContactViewModel]）で、ここは取得を促すだけ。
 * 取得のきっかけが画面の切り替えなので、[AppNavigation] が呼ぶ。
 *
 * [MissedCallViewModel] とまとめないのは、責務が別だから。
 */
@HiltViewModel
class NoticeViewModel @Inject constructor(
    private val noticeManager: NoticeManager,
) : ViewModel() {

    fun refresh() = noticeManager.refresh()
}
