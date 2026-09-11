package com.example.androidsampleapp.core

import com.example.androidsampleapp.domain.model.Aircon
import com.example.androidsampleapp.domain.model.ConnectionState
import com.example.androidsampleapp.domain.model.IncomingCall
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 機器から降ってくる状態の単一管理者。全画面がここを見る。
 *
 * 書き込んでよいのは data 層（受信を反映する DeviceRepositoryImpl）だけ。
 * 画面や ViewModel からは読むだけにする。
 *
 * ここに置くのは「機器の状態」に限る。画面ごとの状態は各画面の XxxState が持ち、
 * スリープ中かどうかは唯一の書き手である IdleTimer が自分で持つ。
 */
@Singleton
class AppStateHolder @Inject constructor() {

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _incomingCall = MutableStateFlow<IncomingCall?>(null)
    val incomingCall: StateFlow<IncomingCall?> = _incomingCall.asStateFlow()

    private val _aircon = MutableStateFlow(Aircon())
    val aircon: StateFlow<Aircon> = _aircon.asStateFlow()

    fun updateConnectionState(state: ConnectionState) {
        _connectionState.value = state
    }

    fun updateIncomingCall(call: IncomingCall?) {
        _incomingCall.value = call
    }

    fun updateAircon(transform: (Aircon) -> Aircon) {
        _aircon.update(transform)
    }
}
