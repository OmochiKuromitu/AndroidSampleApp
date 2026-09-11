package com.example.androidsampleapp.ui.contact

import com.example.androidsampleapp.core.mvi.UiEffect

/**
 * この画面が出す一回きりの出来事は、今のところ無い。
 *
 * 発信は機器側のコマンドが未定のため、行を押しても何もしない。
 * 発信を足すときは、ここに「発信した」「失敗した」を並べる。
 */
sealed interface ContactEffect : UiEffect
