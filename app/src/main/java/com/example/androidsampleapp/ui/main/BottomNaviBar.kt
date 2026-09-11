package com.example.androidsampleapp.ui.main

import androidx.compose.foundation.layout.height
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.androidsampleapp.R
import com.example.androidsampleapp.ui.theme.dimensions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomNaviBar(
    selectedTab: MainTab,
    onTabClick: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
    /** 連絡先タブに出すバッジの件数。0 なら出さない。 */
    missedCallCount: Int = 0,
) {
    NavigationBar(modifier = modifier.height(MaterialTheme.dimensions.bottomBarHeight)) {
        MainTab.entries.forEach { tab ->
            val badgeCount = if (tab == MainTab.CONTACT) missedCallCount else 0
            NavigationBarItem(
                selected = tab == selectedTab,
                onClick = { onTabClick(tab) },
                icon = {
                    BadgedBox(
                        badge = { if (badgeCount > 0) Badge { Text(badgeCount.toString()) } },
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            // 数字だけだと読み上げが「2」になるので、何の件数かを添える。
                            contentDescription = if (badgeCount > 0) {
                                stringResource(R.string.missed_call_badge, badgeCount)
                            } else {
                                stringResource(tab.labelRes)
                            },
                        )
                    }
                },
                label = { Text(stringResource(tab.labelRes)) },
            )
        }
    }
}
