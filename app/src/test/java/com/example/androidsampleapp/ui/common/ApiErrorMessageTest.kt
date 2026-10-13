package com.example.androidsampleapp.ui.common

import com.example.androidsampleapp.R
import com.example.androidsampleapp.domain.model.ApiError
import com.example.androidsampleapp.domain.model.ApiResult
import com.example.androidsampleapp.domain.model.apiError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ApiErrorMessageTest {

    @Test
    fun `成功には文言を出さない`() {
        assertNull(ApiResult.Success(Unit).errorMessageRes())
    }

    @Test
    fun `タイムアウトとサーバエラーはそれぞれの既定の文言`() {
        assertEquals(R.string.api_error_timeout, ApiResult.Timeout.errorMessageRes())
        assertEquals(R.string.api_error_server, ApiResult.ServerError(503).errorMessageRes())
    }

    @Test
    fun `サーバの message は apiError から取れる。タイムアウトには無い`() {
        val error = ApiError("MAINTENANCE", "メンテナンス中です")

        assertEquals(error, ApiResult.ServerError(503, error).apiError)
        assertNull(ApiResult.Timeout.apiError)
    }
}
