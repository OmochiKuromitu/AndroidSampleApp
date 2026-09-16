package com.example.androidsampleapp.domain.usecase

import com.example.androidsampleapp.domain.model.Notice
import com.example.androidsampleapp.domain.repository.NoticeRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/**
 * API の通知一覧を見る。取り直されるたびに新しい値が流れる。まだ取れていなければ null。
 * 機器から届いた通知は [ObserveDeviceNoticesUseCase] が別に流す。
 */
class ObserveNoticesUseCase @Inject constructor(
    private val repository: NoticeRepository,
) {
    operator fun invoke(): StateFlow<List<Notice>?> = repository.notices
}
