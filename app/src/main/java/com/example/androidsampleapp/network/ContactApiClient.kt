package com.example.androidsampleapp.network

import com.example.androidsampleapp.model.ContactResponse
import com.example.androidsampleapp.model.DeviceRequest
import javax.inject.Inject

/** 型の違う成功・失敗を返す呼び出し例。既存の ContactApi をそのまま利用する。 */
class ContactApiClient @Inject constructor(
    private val api: ContactApi,
    private val executor: ApiRequestExecutor,
) {
    suspend fun getContacts(request: DeviceRequest): ApiResult<List<ContactResponse>> =
        executor.execute { api.getContacts(request) }
}
