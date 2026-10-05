package com.example.androidsampleapp.domain.repository

import com.example.androidsampleapp.domain.model.NoticeSnapshot
import kotlinx.coroutines.flow.StateFlow

/**
 * 通知は HTTP の API から取る。API は取得と消去の 2 本。
 *
 * 取った結果と取得状況を [noticeSnapshot] に公開する。画面をまたいで同じ状態を購読する。
 */
interface NoticeRepository {
    /** 最後に取れた通知の一覧（新しいものが先頭）、取得済みかどうか、直近の失敗。 */
    val noticeSnapshot: StateFlow<NoticeSnapshot>

    /**
     * 取得 API を呼び、共有状態を更新する。実行中なら重ねて取得しない。
     * 失敗したら例外を投げ、前回の一覧を残して失敗を記録する。
     */
    suspend fun refreshNotices()

    /**
     * 消去して一覧を取り直す。取得中でも消去操作は取り落とさない。
     * 消去と再取得の間に別の要求を割り込ませず、失敗時は前回の一覧を残して失敗を記録する。
     */
    suspend fun deleteAllNotices()
}
