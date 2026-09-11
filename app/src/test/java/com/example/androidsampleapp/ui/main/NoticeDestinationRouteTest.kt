package com.example.androidsampleapp.ui.main

import com.example.androidsampleapp.domain.model.NoticeDestination
import com.example.androidsampleapp.ui.common.Route
import org.junit.Assert.assertEquals
import org.junit.Test

class NoticeDestinationRouteTest {

    @Test
    fun `不在着信の通知は履歴から開くルートになる`() {
        val route = NoticeDestination.Contact(hasMissedCall = true).toRoute()

        assertEquals(Route.contact(showHistory = true), route)
        assertEquals("contact?list=history", route)
    }

    @Test
    fun `不在でなければ引数を付けない`() {
        val route = NoticeDestination.Contact(hasMissedCall = false).toRoute()

        assertEquals(Route.CONTACT, route)
    }

    @Test
    fun `他の飛び先はそのままのルート`() {
        assertEquals(Route.TOP, NoticeDestination.Top.toRoute())
        assertEquals(Route.AIRCON, NoticeDestination.Aircon.toRoute())
    }

    @Test
    fun `API のコードから不在フラグを読み取る`() {
        assertEquals(
            NoticeDestination.Contact(hasMissedCall = true),
            NoticeDestination.fromCode("CONTACT_MISSED"),
        )
        assertEquals(
            NoticeDestination.Contact(hasMissedCall = false),
            NoticeDestination.fromCode("CONTACT"),
        )
        assertEquals(NoticeDestination.Top, NoticeDestination.fromCode("なにか"))
    }
}
