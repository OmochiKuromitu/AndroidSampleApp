package com.example.androidsampleapp.domain.repository

import com.example.androidsampleapp.domain.model.Notice
import kotlinx.coroutines.flow.StateFlow

interface NoticeRepository {
    /** 新しいものが先頭。 */
    val notices: StateFlow<List<Notice>>

    /** API から取り直す。結果は [notices] に流れる。 */
    suspend fun refresh()

    suspend fun clear()
}
