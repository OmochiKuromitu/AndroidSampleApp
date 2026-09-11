package com.example.androidsampleapp.domain.usecase

import com.example.androidsampleapp.domain.repository.ContactRepository
import javax.inject.Inject

/**
 * 不在着信を既読にしたうえで、取り直した件数を返す。
 *
 * 既読 API と取得 API を 1 つの操作にまとめてあるのは、既読にしている間に届いた分を
 * 落とさないため。呼び出し側は「既読にした結果の件数」が返るとだけ知っていればよい。
 */
class MarkMissedCallsAsReadUseCase @Inject constructor(
    private val repository: ContactRepository,
) {
    suspend operator fun invoke(): Int {
        repository.markMissedCallsAsRead()
        return repository.getMissedCallCount()
    }
}
