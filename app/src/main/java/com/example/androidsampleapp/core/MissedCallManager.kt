package com.example.androidsampleapp.core

import com.example.androidsampleapp.domain.model.Notice
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 不在着信の件数と通知一覧を、画面をまたいで保持する共有状態の器。
 *
 * AppStateHolder と同じく、書き込むのは Repository だけ。
 * ViewModel は Repository と UseCase を通して読む。取得の実行や購読はここでは行わず、
 * コルーチンの寿命は呼び出し元の ViewModel が決める。
 */
@Singleton
class MissedCallManager @Inject constructor() {

    private val _missedCallCount = MutableStateFlow(0)
    val missedCallCount: StateFlow<Int> = _missedCallCount.asStateFlow()

    private val _notices = MutableStateFlow<List<Notice>>(emptyList())
    val notices: StateFlow<List<Notice>> = _notices.asStateFlow()

    internal fun updateMissedCallCount(count: Int) {
        _missedCallCount.value = count
    }

    internal fun updateNotices(notices: List<Notice>) {
        _notices.value = notices
    }
}
