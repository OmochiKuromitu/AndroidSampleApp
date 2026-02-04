package com.example.androidsampleapp.domain.repository

import com.example.androidsampleapp.domain.model.Notice
import kotlinx.coroutines.flow.StateFlow

/**
 * 通知は HTTP の API から取る。API は取得と消去の 2 本。
 *
 * 取った結果を [notices] に持つ。画面はこれを購読し、取り直された値がそのまま反映されるようにする。
 */
interface NoticeRepository {
    /** 最後に取れた通知の一覧（新しいものが先頭）。まだ一度も取れていなければ null。 */
    val notices: StateFlow<List<Notice>?>

    /**
     * 取得 API を呼び、結果を [notices] に入れる。
     * 失敗したら例外を投げ、[notices] は前回の値のまま残す。
     */
    suspend fun refreshNotices()

    /** 消去 API。サーバ側の一覧を空にする。[notices] は取り直すまで変わらない。 */
    suspend fun deleteAllNotices()
}
