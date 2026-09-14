package com.example.androidsampleapp.domain.repository

import com.example.androidsampleapp.domain.model.ConnectionState
import com.example.androidsampleapp.domain.model.IncomingCall
import com.example.androidsampleapp.domain.model.Notice
import kotlinx.coroutines.flow.StateFlow

interface DeviceRepository {
    val connectionState: StateFlow<ConnectionState>
    val incomingCall: StateFlow<IncomingCall?>

    /** 機器から届いた通知。新しいものが先頭。 */
    val deviceNotices: StateFlow<List<Notice>>

    /** 切断されても再接続し続ける。呼び出し元（Service）がキャンセルするまで戻らない。 */
    suspend fun monitor()

    suspend fun answerCall(roomId: String)
    suspend fun rejectCall(roomId: String)

    /** 機器から届いた通知を手元から消す。機器には何も送らない。 */
    fun clearDeviceNotices()
}
