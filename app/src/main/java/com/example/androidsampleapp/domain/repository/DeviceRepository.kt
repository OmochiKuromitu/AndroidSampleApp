package com.example.androidsampleapp.domain.repository

import com.example.androidsampleapp.domain.model.ApiResult
import com.example.androidsampleapp.domain.model.ConnectionState
import com.example.androidsampleapp.domain.model.IncomingCall
import kotlinx.coroutines.flow.StateFlow

interface DeviceRepository {
    /** 状態取得 API に届いているか。直近の取得が成功していれば接続中とみなす。 */
    val connectionState: StateFlow<ConnectionState>
    val incomingCall: StateFlow<IncomingCall?>

    /**
     * 状態取得 API を一定間隔で呼び、接続状態・着信・エアコンの現在値に反映し続ける。
     * 失敗しても間隔を置いて呼び直す。呼び出し元がキャンセルするまで戻らない。
     */
    suspend fun monitor()

    /** 応答 / 拒否 API。成功したら着信を消して [ApiResult.Success] を返す。失敗では着信を残す。 */
    suspend fun answerCall(roomId: String): ApiResult<Unit>
    suspend fun rejectCall(roomId: String): ApiResult<Unit>
}
