package com.example.androidsampleapp.ui.common

import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.androidsampleapp.ui.theme.AppTheme

/**
 * 壁付けパネル想定の横長サイズで、ライトとダークを並べて出す。
 * 画面ごとに @Preview を書き分けず、これを付ける。
 */
@Preview(name = "Light", widthDp = 800, heightDp = 480, showBackground = true)
@Preview(
    name = "Dark",
    widthDp = 800,
    heightDp = 480,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
annotation class PanelPreview

/** 縦に長い画面（スリープ画面のように一覧を抱えるもの）用。 */
@Preview(name = "Light", widthDp = 800, heightDp = 1280, showBackground = true)
@Preview(
    name = "Dark",
    widthDp = 800,
    heightDp = 1280,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
annotation class TallPanelPreview

/**
 * プレビュー用の下敷き。テーマと背景色を実機と揃える。
 *
 * [AppTheme] を通さないと Dimensions とタイポグラフィが既定値になり、
 * 実機と違う見た目のまま調整してしまう。
 */
@Composable
fun PreviewSurface(content: @Composable () -> Unit) {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background, content = content)
    }
}
