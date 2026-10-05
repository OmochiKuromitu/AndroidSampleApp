package com.example.androidsampleapp.domain.repository

import com.example.androidsampleapp.domain.model.AddressBook
import kotlinx.coroutines.flow.StateFlow

/**
 * 連絡先は HTTP の API から取る。電話帳と通話履歴で API が分かれている。
 *
 * 電話帳と履歴は、取った結果を [addressBook] に持つ。画面はこれを購読し、
 * 取り直された値がそのまま反映されるようにする。
 * 不在着信の件数も共有状態として公開し、取得と既読の結果を反映する。
 */
interface ContactRepository {
    /** 最後に取れた電話帳と履歴。まだ一度も取れていなければ null。 */
    val addressBook: StateFlow<AddressBook?>

    /** 最後に取得できた未確認の不在着信の件数。失敗しても前回の値を残す。 */
    val missedCallCount: StateFlow<Int>

    /**
     * 電話帳取得 API と通話履歴取得 API を呼び、結果を [addressBook] に入れる。
     * 失敗したら例外を投げ、[addressBook] は前回の値のまま残す。
     */
    suspend fun refreshAddressBook()

    /** 件数を取得して共有状態を更新する。同じ種類の要求が実行中なら重ねて取得しない。 */
    suspend fun refreshMissedCallCount()

    /**
     * 既読にして件数を取り直す。取得中でも既読操作は取り落とさない。
     * 既読と再取得の間に別の要求を割り込ませず、失敗時は例外を投げて前回の件数を残す。
     */
    suspend fun markMissedCallsAsRead()
}
