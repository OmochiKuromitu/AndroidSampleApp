package com.example.androidsampleapp.ui.common

import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.androidsampleapp.ui.theme.AppTheme

/**
 * 壁付けパネル想定の縦長サイズで、ライトとダークを並べて出す。
 * 画面ごとに @Preview を書き分けず、これを付ける。
 *
 * 実機の寸法に合わせるときはこの 2 行の dp を直す。全画面のプレビューに効く。
 */
@Preview(name = "Light", widthDp = 480, heightDp = 800, showBackground = true)
@Preview(
    name = "Dark",
    widthDp = 480,
    heightDp = 800,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
annotation class PanelPreview

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
