package com.example.androidsampleapp.core

import com.example.androidsampleapp.domain.model.AddressBook
import com.example.androidsampleapp.domain.model.Notice
import com.example.androidsampleapp.domain.repository.ContactRepository
import com.example.androidsampleapp.domain.repository.NoticeRepository
import com.example.androidsampleapp.domain.usecase.MissedCallUseCase
import com.example.androidsampleapp.domain.usecase.NoticeUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// MissedCallManager のテストで使う偽のリポジトリ。どれも 1 回の呼び出しに 100ms かかる。

/** 呼ばれた回数を数える。既読にすると以後は 0 件を返す。 */
internal class FakeContactRepository(
    private val counts: List<Int> = listOf(1, 2, 3),
    private val failOnGet: () -> Boolean = { false },
) : ContactRepository {
    var getCount = 0
        private set
    var readCount = 0
        private set

    override val addressBook: StateFlow<AddressBook?> = MutableStateFlow(null)

    override suspend fun refreshAddressBook() = Unit

    override suspend fun getMissedCallCount(): Int {
        if (failOnGet()) error("通信失敗")
        val index = getCount
        getCount++
        delay(100)
        return if (readCount > 0) 0 else counts.getOrElse(index) { counts.last() }
    }

    override suspend fun markMissedCallsAsRead() {
        readCount++
        delay(100)
    }
}

/** 呼ばれた回数を数える。消去すると以後の取り直しは空になる。 */
internal class FakeNoticeRepository(
    private val serverNotices: List<Notice>,
    private val failOnGet: () -> Boolean = { false },
    private val failOnDelete: Boolean = false,
) : NoticeRepository {
    var getCount = 0
        private set
    var deleteCount = 0
        private set

    override val notices = MutableStateFlow<List<Notice>?>(null)

    override suspend fun refreshNotices() {
        getCount++
        delay(100)
        if (failOnGet()) error("通信失敗")
        notices.value = if (deleteCount > 0) emptyList() else serverNotices
    }

    override suspend fun deleteAllNotices() {
        deleteCount++
        delay(100)
        if (failOnDelete) error("通信失敗")
    }
}

/** 偽のリポジトリから、本物の UseCase を挟んでマネージャーを組み立てる。 */
internal fun missedCallManager(
    contactRepository: ContactRepository = FakeContactRepository(),
    noticeRepository: NoticeRepository = FakeNoticeRepository(emptyList()),
    scope: CoroutineScope,
) = MissedCallManager(
    missedCallUseCase = MissedCallUseCase(contactRepository),
    noticeUseCase = NoticeUseCase(noticeRepository),
    scope = scope,
)
