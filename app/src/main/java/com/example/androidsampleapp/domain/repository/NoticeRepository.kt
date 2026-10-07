package com.example.androidsampleapp.domain.repository

import com.example.androidsampleapp.domain.model.Notice
import kotlinx.coroutines.flow.StateFlow

/**
 * 通知は HTTP の API から取る。API は取得と消去の 2 本。
 *
 * 取った結果を [notices] に公開する。画面をまたいで同じ一覧を購読する。
 */
interface NoticeRepository {
    /** 通知の一覧（新しいものが先頭）。まだ取れていないとき、直近の取得や消去が失敗したときは空。 */
    val notices: StateFlow<List<Notice>>

    /**
     * 取得 API を呼び、共有状態を更新する。実行中なら重ねて取得しない。
     * 失敗したら一覧を空にして例外を投げる。キャンセルされたら一覧に触らない。
     */
    suspend fun refreshNotices()

    /**
     * 消去して一覧を取り直す。取得中でも消去操作は取り落とさない。
     * 消去と再取得の間に別の要求を割り込ませず、失敗したら一覧を空にして例外を投げる。
     */
    suspend fun deleteAllNotices()
}
