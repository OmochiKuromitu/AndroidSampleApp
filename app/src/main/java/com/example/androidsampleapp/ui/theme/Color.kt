package com.example.androidsampleapp.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val Teal900 = Color(0xFF0F3D3A)
val Teal700 = Color(0xFF1B5E5A)
val Teal200 = Color(0xFF80D5CE)
val Amber500 = Color(0xFFE0A93B)
val Red500 = Color(0xFFC0453C)
val Grey900 = Color(0xFF12141A)
val Grey100 = Color(0xFFF3F4F6)

// 通知タグ。分類ごとに 1 色。白文字が乗る前提で彩度を抑えている。
val TagCall = Color(0xFF3F6FB0)
val TagAircon = Color(0xFF2E8B77)
val TagAlert = Color(0xFFC0453C)
val TagInfo = Color(0xFF6B7280)

internal val LightColors = lightColorScheme(
    primary = Teal700,
    secondary = Teal900,
    tertiary = Amber500,
    error = Red500,
    background = Grey100,
)

internal val DarkColors = darkColorScheme(
    primary = Teal200,
    secondary = Teal200,
    tertiary = Amber500,
    error = Red500,
    background = Grey900,
)
