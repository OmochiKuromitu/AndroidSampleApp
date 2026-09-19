package com.example.androidsampleapp.domain.usecase

import com.example.androidsampleapp.domain.repository.DeviceRepository
import com.example.androidsampleapp.domain.repository.NoticeRepository
import javax.inject.Inject

/**
 * 通知一覧を消したうえで、取り直す。結果は [ObserveNoticesUseCase] の側に流れる。
 *
 * 消去と再取得を 1 つの操作として扱うのは、消している間に届いた通知を落とさないため。
 * 呼び出し側は、API が 2 本であることを知らずに済む。
 *
 * 機器から届いた通知も一緒に消す。そちらは [DeviceRepository.deviceNotices] の変化として流れる。
 */
class ClearNoticesUseCase @Inject constructor(
    private val repository: NoticeRepository,
    private val deviceRepository: DeviceRepository,
) {
    suspend operator fun invoke() {
        // API の消去が失敗したら機器側も消さない。片方だけ消えた一覧を見せないため。
        repository.deleteAllNotices()
        deviceRepository.clearDeviceNotices()
        repository.refreshNotices()
    }
}
