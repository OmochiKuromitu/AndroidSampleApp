package com.example.androidsampleapp.data

import com.example.androidsampleapp.domain.model.AirconMode
import com.example.androidsampleapp.domain.model.NoticeCategory
import com.example.androidsampleapp.domain.model.NoticeDestination

// API がやり取りに使う文字列と、アプリの型との対応。
//
// これは「通信の言葉 → アプリの言葉」の変換なので data 層に置く。domain に置くと、
// domain がサーバの文字列を知ることになり、形式が変わったときに domain まで直す羽目になる。
// 複数のリポジトリ（DeviceRepositoryImpl、AirconRepositoryImpl、NoticeRepositoryImpl）が同じ対応を使うので、
// どちらかの private にはせず、ここに 1 つだけ持つ。
//
// 知らない文字列が来たときにどう読むか（既定値）も、読み替えの判断としてここで決める。

/** API から届いたモードの文字列を読む。知らない値なら冷房として扱う。 */
internal fun airconModeOf(code: String): AirconMode = when (code) {
    "COOL" -> AirconMode.COOL
    "HEAT" -> AirconMode.HEAT
    "DRY" -> AirconMode.DRY
    "FAN" -> AirconMode.FAN
    else -> AirconMode.COOL
}

/** API へ送るときのモードの文字列。[airconModeOf] の逆向き。 */
internal fun AirconMode.toCode(): String = when (this) {
    AirconMode.COOL -> "COOL"
    AirconMode.HEAT -> "HEAT"
    AirconMode.DRY -> "DRY"
    AirconMode.FAN -> "FAN"
}

/**
 * 通知の分類の文字列を読む。大文字小文字は区別しない。知らない値ならお知らせとして扱う。
 *
 * enum の名前（[NoticeCategory.name]）には頼らない。名前を変えただけで通信が壊れるのを防ぐため。
 */
internal fun noticeCategoryOf(code: String): NoticeCategory = when (code.uppercase()) {
    "CALL" -> NoticeCategory.CALL
    "AIRCON" -> NoticeCategory.AIRCON
    "ALERT" -> NoticeCategory.ALERT
    else -> NoticeCategory.INFO
}

/** 通知の飛び先の文字列を読む。大文字小文字は区別しない。知らない値ならトップへ。 */
internal fun noticeDestinationOf(code: String): NoticeDestination = when (code.uppercase()) {
    "AIRCON" -> NoticeDestination.Aircon
    "CONTACT" -> NoticeDestination.Contact(hasMissedCall = false)
    "CONTACT_MISSED" -> NoticeDestination.Contact(hasMissedCall = true)
    else -> NoticeDestination.Top
}
