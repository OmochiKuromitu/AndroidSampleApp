package com.example.androidsampleapp.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NoticeRepositoryImplTest {

    @Test
    fun `取り直すまでは一覧が null`() {
        assertNull(NoticeRepositoryImpl().notices.value)
    }

    @Test
    fun `取り直すと一覧が入る`() = runTest {
        val repository = NoticeRepositoryImpl()

        repository.refreshNotices()

        assertEquals(5, repository.notices.value?.size)
    }

    @Test
    fun `消去しただけでは一覧は変わらず、取り直すと空になる`() = runTest {
        // 消去と取り直しをまとめるのは ClearNoticesUseCase の役目。リポジトリは呼ばれた API だけを反映する。
        val repository = NoticeRepositoryImpl()
        repository.refreshNotices()

        repository.deleteAllNotices()
        assertEquals(5, repository.notices.value?.size)

        repository.refreshNotices()
        assertTrue(repository.notices.value!!.isEmpty())
    }
}
