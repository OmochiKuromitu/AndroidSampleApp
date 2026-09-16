package com.example.androidsampleapp.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ContactRepositoryImplTest {

    @Test
    fun `取り直すまでは電話帳と履歴が null`() {
        // 画面が「まだ取っていない」と「取ったら空だった」を区別できるようにする。
        assertNull(ContactRepositoryImpl().addressBook.value)
    }

    @Test
    fun `取り直すと電話帳と履歴がそろって入る`() = runTest {
        val repository = ContactRepositoryImpl()

        repository.refreshAddressBook()

        val book = repository.addressBook.value
        assertNotNull(book)
        assertEquals(4, book!!.contacts.size)
        assertEquals(4, book.histories.size)
    }
}
