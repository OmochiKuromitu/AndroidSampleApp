package com.example.androidsampleapp.domain.usecase

import com.example.androidsampleapp.domain.model.Notice
import com.example.androidsampleapp.domain.repository.DeviceRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/** 機器から届いた通知を見る。届くたびに新しい一覧が流れる。 */
class ObserveDeviceNoticesUseCase @Inject constructor(
    private val repository: DeviceRepository,
) {
    operator fun invoke(): StateFlow<List<Notice>> = repository.deviceNotices
}
