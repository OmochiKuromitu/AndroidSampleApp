package com.example.androidsampleapp.domain.usecase

import com.example.androidsampleapp.domain.repository.NoticeRepository
import javax.inject.Inject

/**
 * API の通知一覧を取り直す。結果は [ObserveNoticesUseCase] の側に流れる。
 * 失敗したら例外を投げる。
 */
class RefreshNoticesUseCase @Inject constructor(
    private val repository: NoticeRepository,
) {
    suspend operator fun invoke() = repository.refreshNotices()
}
