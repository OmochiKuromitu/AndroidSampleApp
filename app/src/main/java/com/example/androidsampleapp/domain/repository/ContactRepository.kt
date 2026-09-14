package com.example.androidsampleapp.domain.repository

import com.example.androidsampleapp.domain.model.AddressBook
import kotlinx.coroutines.flow.StateFlow

/**
 * 連絡先は HTTP の API から取る。電話帳と通話履歴で API が分かれている。
 *
 * 電話帳と履歴は、取った結果を [addressBook] に持つ。画面はこれを購読し、
 * 取り直された値がそのまま反映されるようにする。
 * 不在着信の件数は MissedCallManager が保持するので、ここは呼ばれたら都度取りに行くだけ。
 */
interface ContactRepository {
    /** 最後に取れた電話帳と履歴。まだ一度も取れていなければ null。 */
    val addressBook: StateFlow<AddressBook?>

    /**
     * 電話帳取得 API と通話履歴取得 API を呼び、結果を [addressBook] に入れる。
     * 失敗したら例外を投げ、[addressBook] は前回の値のまま残す。
     */
    suspend fun refreshAddressBook()

    /** 不在着信取得 API。未確認の件数を返す。 */
    suspend fun getMissedCallCount(): Int

    /** 不在着信既読 API。未確認の印を消す。 */
    suspend fun markMissedCallsAsRead()
}
