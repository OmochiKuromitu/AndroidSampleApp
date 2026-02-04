package com.example.androidsampleapp.ui.main

import com.example.androidsampleapp.ui.common.Route
import org.junit.Assert.assertEquals
import org.junit.Test

class MainTabTest {

    @Test
    fun `ルート文字列からタブを引ける`() {
        assertEquals(MainTab.TOP, MainTab.fromRoute(Route.TOP))
        assertEquals(MainTab.AIRCON, MainTab.fromRoute(Route.AIRCON))
    }

    @Test
    fun `すべてのタブが自分のルートから引ける`() {
        // Route に足したのに MainTab へ足し忘れる、あるいはルート文字列が重複すると落ちる。
        MainTab.entries.forEach { tab ->
            assertEquals(tab, MainTab.fromRoute(tab.route))
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `対応の無いルートは落とす`() {
        MainTab.fromRoute("unknown")
    }
}
