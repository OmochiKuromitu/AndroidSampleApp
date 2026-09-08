package com.example.androidsampleapp.domain.repository

import com.example.androidsampleapp.domain.model.ConnectionState
import com.example.androidsampleapp.domain.model.IncomingCall
import kotlinx.coroutines.flow.StateFlow

interface DeviceRepository {
    val connectionState: StateFlow<ConnectionState>
    val incomingCall: StateFlow<IncomingCall?>

    /** 切断されても再接続し続ける。呼び出し元（Service）がキャンセルするまで戻らない。 */
    suspend fun monitor()

    suspend fun answerCall(roomId: String)
    suspend fun rejectCall(roomId: String)
}
