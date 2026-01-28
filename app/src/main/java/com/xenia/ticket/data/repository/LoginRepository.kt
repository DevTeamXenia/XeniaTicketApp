package com.xenia.ticket.data.repository

import com.xenia.ticket.data.network.model.LoginRequest
import com.xenia.ticket.data.network.model.LoginResponse
import com.xenia.ticket.data.network.service.ApiClient

class LoginRepository {
    suspend fun login(userName: String, password: String): LoginResponse {
        return ApiClient.apiService.login(
            LoginRequest(
                UserName = userName,
                Password = password
            )
        )
    }

}