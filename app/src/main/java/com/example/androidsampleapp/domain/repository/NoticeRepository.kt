package com.example.androidsampleapp.domain.repository

import com.example.androidsampleapp.domain.model.Notice
import kotlinx.coroutines.flow.StateFlow

interface NoticeRepository {
    /** 新しいものが先頭。 */
    val notices: StateFlow<List<Notice>>

    suspend fun clear()
}
