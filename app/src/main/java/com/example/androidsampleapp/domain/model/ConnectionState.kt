package com.example.androidsampleapp.domain.model

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ;

    val isConnected: Boolean get() = this == CONNECTED
}
