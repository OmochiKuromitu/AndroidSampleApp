package com.example.androidsampleapp.domain.repository

import com.example.androidsampleapp.domain.model.CallHistory
import com.example.androidsampleapp.domain.model.Contact

/**
 * 連絡先は HTTP の API から取る。電話帳と通話履歴で API が分かれている。
 * 状態は持たず、呼ばれたら都度取りに行く。保持するのは画面の State 側。
 */
interface ContactRepository {
    /** 電話帳取得 API。 */
    suspend fun getContacts(): List<Contact>

    /** 通話履歴取得 API。新しいものが先頭。 */
    suspend fun getCallHistories(): List<CallHistory>

    /** 不在着信取得 API。未確認の件数を返す。 */
    suspend fun getMissedCallCount(): Int
}
