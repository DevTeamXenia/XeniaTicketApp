package com.example.ticket.data.repository

import com.example.ticket.data.network.model.LoginRequest
import com.example.ticket.data.network.model.LoginResponse
import com.example.ticket.data.network.service.ApiClient

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