package com.example.androidsampleapp.data

import com.example.androidsampleapp.domain.model.AirconMode
import com.example.androidsampleapp.domain.model.NoticeCategory
import com.example.androidsampleapp.domain.model.NoticeDestination
import org.junit.Assert.assertEquals
import org.junit.Test

class CodeMappingTest {

    @Test
    fun `モードは送る文字列と読む文字列が対になっている`() {
        // 送ったモードが、機器から返ってきたときに同じモードとして読めること。
        AirconMode.entries.forEach { mode ->
            assertEquals(mode, airconModeOf(mode.toCode()))
        }
    }

    @Test
    fun `知らないモードは冷房として読む`() {
        assertEquals(AirconMode.COOL, airconModeOf("TURBO"))
    }

    @Test
    fun `通知の分類を大文字小文字を問わず読む`() {
        assertEquals(NoticeCategory.ALERT, noticeCategoryOf("ALERT"))
        assertEquals(NoticeCategory.CALL, noticeCategoryOf("call"))
    }

    @Test
    fun `知らない分類はお知らせとして読む`() {
        assertEquals(NoticeCategory.INFO, noticeCategoryOf("なにか"))
    }

    @Test
    fun `飛び先のコードから不在フラグを読み取る`() {
        assertEquals(
            NoticeDestination.Contact(hasMissedCall = true),
            noticeDestinationOf("CONTACT_MISSED"),
        )
        assertEquals(
            NoticeDestination.Contact(hasMissedCall = false),
            noticeDestinationOf("CONTACT"),
        )
        assertEquals(NoticeDestination.Aircon, noticeDestinationOf("aircon"))
    }

    @Test
    fun `知らない飛び先はトップにする`() {
        assertEquals(NoticeDestination.Top, noticeDestinationOf("なにか"))
    }
}
