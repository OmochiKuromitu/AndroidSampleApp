package com.example.androidsampleapp.domain.usecase

import com.example.androidsampleapp.domain.repository.ContactRepository
import javax.inject.Inject

/**
 * 不在着信の件数まわりの操作。件数を取る、既読にする、を 1 か所にまとめる。
 *
 * 呼ぶ順番（既読 → 件数の取り直し）はここが決める。
 * いつ呼ぶか、重なった要求をどう扱うか、失敗をどう見せるかは呼び出し側が決める。
 */
class MissedCallUseCase @Inject constructor(
    private val repository: ContactRepository,
) {
    /** 未確認の不在着信の件数。失敗したら例外を投げる。 */
    suspend fun getCount(): Int = repository.getMissedCallCount()

    /**
     * 既読にして、取り直した件数を返す。失敗したら例外を投げる。
     *
     * 0 と決め打ちせずに取り直すのは、既読にしている間に届いた分を落とさないため。
     */
    suspend fun markAsRead(): Int {
        repository.markMissedCallsAsRead()
        return repository.getMissedCallCount()
    }
}
