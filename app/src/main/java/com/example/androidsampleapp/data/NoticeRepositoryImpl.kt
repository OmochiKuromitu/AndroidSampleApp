package com.example.androidsampleapp.data

import com.example.androidsampleapp.core.AppStateHolder
import com.example.androidsampleapp.domain.repository.NoticeRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 通知の保持は [AppStateHolder] に任せ、ここは読み口と消去だけ。
 * 受信して積むのは [DeviceRepositoryImpl] の仕事。
 */
@Singleton
class NoticeRepositoryImpl @Inject constructor(
    private val appStateHolder: AppStateHolder,
) : NoticeRepository {

    override val notices = appStateHolder.notices

    override suspend fun clear() {
        appStateHolder.clearNotices()
    }
}
