package com.example.androidsampleapp.ui.contact

import com.example.androidsampleapp.core.mvi.UiEffect
import com.example.androidsampleapp.domain.model.NoticeDestination

/**
 * この画面が出す一回きりの出来事。
 *
 * 発信は機器側のコマンドが未定のため、行を押しても何もしない。
 * 発信を足すときは、ここに「発信した」「失敗した」を並べる。
 */
sealed interface ContactEffect : UiEffect {
    /** お知らせが選ばれた。どこへ行くかは AppNavigation が決める。 */
    data class NoticeSelected(val destination: NoticeDestination) : ContactEffect
}
