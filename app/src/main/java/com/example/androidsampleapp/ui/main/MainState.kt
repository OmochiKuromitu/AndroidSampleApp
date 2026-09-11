package com.example.androidsampleapp.ui.main

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.androidsampleapp.R
import com.example.androidsampleapp.core.mvi.UiState
import com.example.androidsampleapp.domain.model.ConnectionState
import com.example.androidsampleapp.domain.model.NoticeDestination
import com.example.androidsampleapp.ui.common.Route

/**
 * 下部バーのタブ。ここに 1 件足せばタブが 1 つ増える。
 * SLEEP は画面を出すのではなくスリープ状態に入れるだけなので、扱いが他と異なる。
 */
enum class MainTab(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    TOP(Route.TOP, R.string.tab_top, Icons.Filled.Home),
    AIRCON(Route.AIRCON, R.string.tab_aircon, Icons.Filled.AcUnit),
    CONTACT(Route.CONTACT, R.string.tab_contact, Icons.Filled.Contacts),
    SLEEP(Route.SLEEP, R.string.tab_sleep, Icons.Filled.Bedtime),
    ;

    companion object {
        /**
         * ルート文字列に対応するタブを引く。
         *
         * 行き先を「ルート」と「タブ」で二重に書かないためのもの。二重に書くと、
         * 片方だけ直したときにヘッダーの見出しと下部バーのハイライトがずれる。
         * 対応が無いのは [Route] への追加漏れなので、その場で落として気づけるようにする。
         */
        fun fromRoute(route: String): MainTab =
            entries.firstOrNull { it.route == route }
                ?: error("$route に対応する MainTab がない")
    }
}

/**
 * 通知の飛び先をルート文字列に対応させる。ドメインは画面の住所を知らないので、変換はここに置く。
 *
 * 不在着信だったかどうかは飛び先の値として渡ってくるが、「だから履歴タブを開く」と
 * 決めるのはここ。ドメインは不在だったという事実しか持たない。
 */
fun NoticeDestination.toRoute(): String = when (this) {
    NoticeDestination.Top -> Route.TOP
    NoticeDestination.Aircon -> Route.AIRCON
    is NoticeDestination.Contact -> Route.contact(showHistory = hasMissedCall)
}

/**
 * メイン画面の枠が持つ状態。
 *
 * 選択中のタブはここに無い。どのタブを表示しているかは NavController の現在地であって
 * 画面の状態ではないため、AppNavigation が決めて引数で渡す。
 */
data class MainState(
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
) : UiState
