package com.example.androidsampleapp.domain.usecase

import com.example.androidsampleapp.domain.model.Notice
import com.example.androidsampleapp.domain.repository.NoticeRepository
import javax.inject.Inject

/**
 * 通知一覧を消したうえで、取り直した一覧を返す。
 *
 * 消去と再取得を 1 つの操作として扱うのは、消している間に届いた通知を落とさないため。
 * 呼び出し側から見れば「消した結果の一覧」が返るだけで、API が 2 本であることを知らずに済む。
 */
class ClearNoticesUseCase @Inject constructor(
    private val repository: NoticeRepository,
) {
    suspend operator fun invoke(): List<Notice> {
        repository.deleteAllNotices()
        return repository.getNotices()
    }
}
