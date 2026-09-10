package com.example.androidsampleapp.ui.main

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.androidsampleapp.R
import com.example.androidsampleapp.core.mvi.UiState
import com.example.androidsampleapp.domain.model.ConnectionState
import com.example.androidsampleapp.domain.model.NoticeDestination
import com.example.androidsampleapp.ui.common.Route

/**
 * 下部バーのタブ。ここに 1 件足せばタブが 1 つ増える。
 * SLEEP だけは表示先が別のトップレベルルートなので、扱いが他と異なる。
 */
enum class MainTab(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    TOP(Route.TOP, R.string.tab_top, Icons.Filled.Home),
    AIRCON(Route.AIRCON, R.string.tab_aircon, Icons.Filled.AcUnit),
    SLEEP(Route.SLEEP, R.string.tab_sleep, Icons.Filled.Bedtime),
}

/**
 * 通知の飛び先をタブに対応させる。ドメインは画面の住所を知らないので、変換はここに置く。
 */
fun NoticeDestination.toMainTab(): MainTab = when (this) {
    NoticeDestination.TOP -> MainTab.TOP
    NoticeDestination.AIRCON -> MainTab.AIRCON
}

data class MainState(
    /** 表示中のコンテンツタブ。SLEEP はここに入らない。 */
    val selectedTab: MainTab = MainTab.TOP,
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
) : UiState
