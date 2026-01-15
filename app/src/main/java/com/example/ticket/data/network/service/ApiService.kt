package com.example.ticket.data.network.service

import com.example.ticket.data.network.model.CompanyResponse
import com.example.ticket.data.network.model.LoginRequest
import com.example.ticket.data.network.model.LoginResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {

    @POST("Auth/login")
    suspend fun login(
        @Body body: LoginRequest
    ): LoginResponse

    @GET("Company/setting")
    suspend fun getCompanySettings(
        @Header("Authorization") bearerToken: String
    ): List<CompanyResponse>


}