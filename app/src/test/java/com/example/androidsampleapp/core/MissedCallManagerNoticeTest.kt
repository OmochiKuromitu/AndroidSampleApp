package com.example.androidsampleapp.core

import com.example.androidsampleapp.domain.model.Notice
import com.example.androidsampleapp.domain.model.NoticeCategory
import com.example.androidsampleapp.domain.model.NoticeDestination
import com.example.androidsampleapp.domain.repository.NoticeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** [MissedCallManager] のうち、通知一覧の側。件数の側は [MissedCallManagerTest]。 */
@OptIn(ExperimentalCoroutinesApi::class)
class MissedCallManagerNoticeTest {

    private fun notice(id: String, occurredAt: Long, category: NoticeCategory = NoticeCategory.INFO) =
        Notice(id, category, null, "本文", occurredAt, NoticeDestination.Top)

    private fun manager(
        repository: NoticeRepository,
        scope: CoroutineScope,
    ) = missedCallManager(noticeRepository = repository, scope = scope)

    /** 実行中の要求が終わるところまで仮想時間を進める。理由は MissedCallManagerTest を参照。 */
    private fun TestScope.settle() {
        advanceTimeBy(SETTLE_MS)
        runCurrent()
    }

    @Test
    fun `refresh で API の一覧が入る`() = runTest {
        val api = listOf(notice("1", 100L))
        val manager = manager(FakeNoticeRepository(api), backgroundScope)

        manager.refresh()
        settle()

        assertEquals(api, manager.noticeSnapshot.value.notices)
        assertFalse(manager.noticeSnapshot.value.isLoading)
    }

    @Test
    fun `API の通知を一度も受け取っていない間は読み込み中`() = runTest {
        val manager = manager(FakeNoticeRepository(emptyList()), backgroundScope)

        manager.refresh()
        runCurrent()

        assertTrue(manager.noticeSnapshot.value.isLoading)
    }

    @Test
    fun `取ったら空だった場合も読み込み中が解ける`() = runTest {
        // 「まだ取っていない空」と区別できないと、空のときに読み込み中のまま止まる。
        val manager = manager(FakeNoticeRepository(emptyList()), backgroundScope)

        manager.refresh()
        settle()

        assertTrue(manager.noticeSnapshot.value.notices.isEmpty())
        assertFalse(manager.noticeSnapshot.value.isLoading)
    }

    @Test
    fun `受け取り済みなら取り直しの間も読み込み中にしない`() = runTest {
        // 前回の一覧を出したまま取り直す。
        val api = listOf(notice("1", 100L))
        val manager = manager(FakeNoticeRepository(api), backgroundScope)
        manager.refresh()
        settle()

        manager.refresh()
        runCurrent()

        assertEquals(api, manager.noticeSnapshot.value.notices)
        assertFalse(manager.noticeSnapshot.value.isLoading)
    }

    @Test
    fun `一覧を新しい順に並べる`() = runTest {
        val manager = manager(
            FakeNoticeRepository(listOf(notice("old", 100L), notice("new", 300L), notice("mid", 200L))),
            backgroundScope,
        )

        manager.refresh()
        settle()

        assertEquals(listOf("new", "mid", "old"), manager.noticeSnapshot.value.notices.map { it.id })
    }

    @Test
    fun `実行中に重ねて呼んでも要求は 1 本にまとまる`() = runTest {
        val repository = FakeNoticeRepository(emptyList())
        val manager = manager(repository, backgroundScope)

        manager.refresh()
        manager.refresh()
        manager.refresh()
        settle()

        assertEquals(1, repository.getCount)
    }

    @Test
    fun `初回の取得に失敗したら読み込み中が解けて失敗が立つ`() = runTest {
        val manager = manager(
            FakeNoticeRepository(emptyList(), failOnGet = { true }),
            backgroundScope,
        )

        manager.refresh()
        settle()

        assertTrue(manager.noticeSnapshot.value.loadFailed)
        assertFalse(manager.noticeSnapshot.value.isLoading)
    }

    @Test
    fun `失敗のあとに取り直せたら失敗が消える`() = runTest {
        var shouldFail = true
        val manager = manager(
            FakeNoticeRepository(listOf(notice("1", 100L)), failOnGet = { shouldFail }),
            backgroundScope,
        )
        manager.refresh()
        settle()

        shouldFail = false
        manager.refresh()
        settle()

        assertFalse(manager.noticeSnapshot.value.loadFailed)
        assertEquals(listOf("1"), manager.noticeSnapshot.value.notices.map { it.id })
    }

    @Test
    fun `取得に失敗しても前回の一覧を残して失敗を立てる`() = runTest {
        var shouldFail = false
        val api = listOf(notice("1", 100L))
        val manager = manager(
            FakeNoticeRepository(api, failOnGet = { shouldFail }),
            backgroundScope,
        )
        manager.refresh()
        settle()

        shouldFail = true
        manager.refresh()
        settle()

        assertEquals(api, manager.noticeSnapshot.value.notices)
        assertTrue(manager.noticeSnapshot.value.loadFailed)
        assertFalse(manager.noticeSnapshot.value.isLoading)
    }

    @Test
    fun `消去は取得中でも取りやめない`() = runTest {
        // 利用者の操作の結果なので、落とすと消したのに残る。
        val repository = FakeNoticeRepository(listOf(notice("1", 100L)))
        val manager = manager(repository, backgroundScope)

        manager.refresh()
        manager.clearNotices()
        settle()

        assertEquals(1, repository.deleteCount)
        assertTrue(manager.noticeSnapshot.value.notices.isEmpty())
        assertFalse(manager.noticeSnapshot.value.loadFailed)
    }

    @Test
    fun `消去に失敗したら一覧を残して失敗を立て、取り直さない`() = runTest {
        val repository = FakeNoticeRepository(listOf(notice("1", 100L)), failOnDelete = true)
        val manager = manager(repository, backgroundScope)
        manager.refresh()
        settle()

        manager.clearNotices()
        settle()

        assertEquals(listOf("1"), manager.noticeSnapshot.value.notices.map { it.id })
        assertEquals(1, repository.getCount)
        assertTrue(manager.noticeSnapshot.value.loadFailed)
    }

    // ---- 件数と通知を 1 つのマネージャーにまとめたことで起きうること ----

    @Test
    fun `refresh 1 回で件数と一覧の両方を取る`() = runTest {
        val contact = FakeContactRepository(counts = listOf(3))
        val repository = FakeNoticeRepository(listOf(notice("1", 100L)))
        val manager = missedCallManager(contact, repository, backgroundScope)

        manager.refresh()
        settle()

        assertEquals(3, manager.missedCallCount.value)
        assertEquals(listOf("1"), manager.noticeSnapshot.value.notices.map { it.id })
    }

    @Test
    fun `通知の消去は不在着信の取得を打ち切らない`() = runTest {
        // 実行中の要求を種類ごとに持っていないと、消去が件数の取得まで止めてしまう。
        val contact = FakeContactRepository(counts = listOf(3))
        val repository = FakeNoticeRepository(listOf(notice("1", 100L)))
        val manager = missedCallManager(contact, repository, backgroundScope)

        manager.refresh()
        manager.clearNotices()
        settle()

        assertEquals(3, manager.missedCallCount.value)
        assertEquals(1, contact.getCount)
    }

    @Test
    fun `既読は通知の取得を打ち切らない`() = runTest {
        val contact = FakeContactRepository(counts = listOf(3))
        val repository = FakeNoticeRepository(listOf(notice("1", 100L)))
        val manager = missedCallManager(contact, repository, backgroundScope)

        manager.refresh()
        manager.markAsRead()
        settle()

        assertEquals(listOf("1"), manager.noticeSnapshot.value.notices.map { it.id })
        assertEquals(1, repository.getCount)
        assertEquals(0, manager.missedCallCount.value)
    }

    private companion object {
        /** 偽リポジトリの遅延（100ms）を消去と取り直しの 2 回分越えられる長さ。 */
        const val SETTLE_MS = 1_000L
    }
}
