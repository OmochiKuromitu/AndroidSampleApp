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
 * 全画面が共有する状態の単一管理者。
 *
 * 書き込んでよいのは data 層（機器からの受信を反映する DeviceRepositoryImpl）と
 * ui/navigation の IdleTimer だけ。画面や ViewModel からは読むだけにする。
 * 画面ごとの状態は各 ui/<feature>/*State が持ち、ここには置かない。
 */
@Singleton
class AppStateHolder @Inject constructor() {

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _incomingCall = MutableStateFlow<IncomingCall?>(null)
    val incomingCall: StateFlow<IncomingCall?> = _incomingCall.asStateFlow()

    private val _aircon = MutableStateFlow(Aircon())
    val aircon: StateFlow<Aircon> = _aircon.asStateFlow()

    private val _isSleeping = MutableStateFlow(false)
    val isSleeping: StateFlow<Boolean> = _isSleeping.asStateFlow()

    fun updateConnectionState(state: ConnectionState) {
        _connectionState.value = state
    }

    fun updateIncomingCall(call: IncomingCall?) {
        _incomingCall.value = call
    }

    fun updateAircon(transform: (Aircon) -> Aircon) {
        _aircon.update(transform)
    }

    fun updateSleeping(isSleeping: Boolean) {
        _isSleeping.value = isSleeping
    }
}
