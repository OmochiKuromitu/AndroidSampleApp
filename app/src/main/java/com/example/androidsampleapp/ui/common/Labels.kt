package com.example.androidsampleapp.ui.common

import androidx.annotation.StringRes
import com.example.androidsampleapp.R
import com.example.androidsampleapp.domain.model.AirconMode
import com.example.androidsampleapp.domain.model.NoticeCategory

// ドメインの値を画面に出すときの表示名。
//
// 「アプリの言葉 → 画面での見せ方」なので ui 層に置く。domain に日本語の表示名を持たせると、
// 言い回しや多言語の対応で domain を触ることになる。MainTab や ContactList と同じく @StringRes で持つ。
// 複数の画面（エアコン画面とトップ画面、スリープ画面の通知一覧）が使うので common に置く。

@StringRes
fun AirconMode.labelRes(): Int = when (this) {
    AirconMode.COOL -> R.string.aircon_mode_cool
    AirconMode.HEAT -> R.string.aircon_mode_heat
    AirconMode.DRY -> R.string.aircon_mode_dry
    AirconMode.FAN -> R.string.aircon_mode_fan
}

@StringRes
fun NoticeCategory.labelRes(): Int = when (this) {
    NoticeCategory.CALL -> R.string.notice_category_call
    NoticeCategory.AIRCON -> R.string.notice_category_aircon
    NoticeCategory.ALERT -> R.string.notice_category_alert
    NoticeCategory.INFO -> R.string.notice_category_info
}
