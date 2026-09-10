package com.example.androidsampleapp.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 余白とタップ領域。数値をここに集約し、画面側で dp をべた書きしない。
 */
data class Dimensions(
    val spaceSmall: Dp = 8.dp,
    val spaceMedium: Dp = 16.dp,
    val spaceLarge: Dp = 24.dp,
    val spaceXLarge: Dp = 40.dp,
    val headerHeight: Dp = 64.dp,
    val bottomBarHeight: Dp = 88.dp,
    /** 手袋でも押せるように、Material の既定 48dp より大きく取る。 */
    val minTouchTarget: Dp = 64.dp,
    val cardCorner: Dp = 16.dp,
    /** スリープ画面で時刻表示を置く、上端からの距離。 */
    val sleepClockTop: Dp = 100.dp,
    /** スリープ解除のスワイプを受け付ける、画面下端の帯の高さ。 */
    val unlockAreaHeight: Dp = 200.dp,
    /** 解除に必要な上方向の移動量。これを超えたら解除する。 */
    val unlockDistance: Dp = 120.dp,
)

val LocalDimensions = staticCompositionLocalOf { Dimensions() }
