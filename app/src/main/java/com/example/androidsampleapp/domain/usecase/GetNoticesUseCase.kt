package com.example.androidsampleapp.domain.usecase

import com.example.androidsampleapp.domain.model.Notice
import com.example.androidsampleapp.domain.repository.NoticeRepository
import javax.inject.Inject

/** 通知一覧を取得 API から取る。 */
class GetNoticesUseCase @Inject constructor(
    private val repository: NoticeRepository,
) {
    suspend operator fun invoke(): List<Notice> = repository.getNotices()
}
