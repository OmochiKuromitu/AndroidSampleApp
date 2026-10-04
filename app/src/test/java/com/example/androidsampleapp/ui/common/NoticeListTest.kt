package com.example.androidsampleapp.ui.common

import com.example.androidsampleapp.domain.model.Notice
import com.example.androidsampleapp.domain.model.NoticeCategory
import com.example.androidsampleapp.domain.model.NoticeDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoticeListTest {

    private fun notice(id: String, occurredAt: Long, isPinned: Boolean = false) =
        Notice(id, NoticeCategory.INFO, null, "本文", occurredAt, NoticeDestination.Top, isPinned)

    @Test
    fun `フラグ付きは上に、それ以外は下に分ける`() {
        val (pinned, others) = splitPinned(
            listOf(notice("a", 300L), notice("p", 100L, isPinned = true), notice("b", 200L)),
        )

        assertEquals(listOf("p"), pinned.map { it.id })
        assertEquals(listOf("a", "b"), others.map { it.id })
    }

    @Test
    fun `フラグ無しは起きた日時の新しい順に並べる`() {
        // 受け取った順が古い順でも、新しい順に並べ直す。
        val (_, others) = splitPinned(
            listOf(notice("old", 100L), notice("new", 300L), notice("mid", 200L)),
        )

        assertEquals(listOf("new", "mid", "old"), others.map { it.id })
    }

    @Test
    fun `フラグ付きの中は受け取った順のまま`() {
        val (pinned, _) = splitPinned(
            listOf(notice("p-old", 100L, isPinned = true), notice("p-new", 300L, isPinned = true)),
        )

        assertEquals(listOf("p-old", "p-new"), pinned.map { it.id })
    }

    @Test
    fun `フラグ付きが無ければ上の区画は空`() {
        val (pinned, others) = splitPinned(listOf(notice("a", 100L)))

        assertTrue(pinned.isEmpty())
        assertEquals(listOf("a"), others.map { it.id })
    }
}
