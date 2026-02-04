package com.example.androidsampleapp.domain.usecase

import com.example.androidsampleapp.domain.model.Notice
import com.example.androidsampleapp.domain.repository.NoticeRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/**
 * 通知まわりの操作。見る、取り直す、消す、を 1 か所にまとめる。
 *
 * 呼ぶ順番（消去 → 取り直し）はここが決める。
 * いつ呼ぶか、重なった要求をどう扱うか、失敗をどう見せるかは呼び出し側（MissedCallManager）が決める。
 */
class NoticeUseCase @Inject constructor(
    private val repository: NoticeRepository,
) {
    /** 通知一覧。取り直されるたびに新しい値が流れる。まだ一度も取れていなければ null。 */
    fun observe(): StateFlow<List<Notice>?> = repository.notices

    /** 通知一覧を取り直す。結果は [observe] の側に流れる。失敗したら例外を投げる。 */
    suspend fun refresh() = repository.refreshNotices()

    /**
     * 通知を消し、取り直す。結果は [observe] の側に流れる。失敗したら例外を投げる。
     *
     * 消したあとに取り直すのは、消している間に届いた通知を落とさないため。
     * 呼び出し側は、API が 2 本であることを知らずに済む。
     */
    suspend fun clear() {
        repository.deleteAllNotices()
        repository.refreshNotices()
    }
}
