package com.example.ticket.data.network.service

import com.example.ticket.data.network.model.CategoryResponse
import com.example.ticket.data.network.model.CompanyResponse
import com.example.ticket.data.network.model.LoginRequest
import com.example.ticket.data.network.model.LoginResponse
import com.example.ticket.data.network.model.TicketResponse
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

    @GET("Category/sync")
    suspend fun getCategory(
        @Header("Authorization") bearerToken: String
    ): CategoryResponse

    @GET(" Ticket/sync")
    suspend fun getTicket(
        @Header("Authorization") bearerToken: String
    ): TicketResponse


}