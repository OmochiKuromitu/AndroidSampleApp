package com.example.androidsampleapp.domain.repository

import com.example.androidsampleapp.domain.model.Notice

/**
 * 通知は HTTP の API から取る。API は取得と消去の 2 本。
 * 状態は持たず、呼ばれたら都度取りに行く。保持するのは画面の State 側。
 */
interface NoticeRepository {
    /** 取得 API。新しいものが先頭。 */
    suspend fun getNotices(): List<Notice>

    /** 消去 API。一覧を空にする。 */
    suspend fun deleteAllNotices()
}
