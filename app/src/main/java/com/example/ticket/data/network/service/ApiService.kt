package com.example.ticket.data.network.service

import com.example.ticket.data.network.model.CategoryResponse
import com.example.ticket.data.network.model.CompanyResponse
import com.example.ticket.data.network.model.LabelSettingsResponse
import com.example.ticket.data.network.model.LoginRequest
import com.example.ticket.data.network.model.LoginResponse
import com.example.ticket.data.network.model.LogoutResponse
import com.example.ticket.data.network.model.OrderResponse
import com.example.ticket.data.network.model.TicketPaymentRequest
import com.example.ticket.data.network.model.TicketRequest
import com.example.ticket.data.network.model.TicketResponse
import okhttp3.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {

    @POST("Auth/login")
    suspend fun login(
        @Body body: LoginRequest
    ): LoginResponse


    @POST("Auth/logout")
    suspend fun logout(
        @Header("Authorization") bearerToken: String
    ): LogoutResponse


    @GET("Company/setting")
    suspend fun getCompanySettings(
        @Header("Authorization") bearerToken: String
    ): List<CompanyResponse>

    @GET("Company/label")
    suspend fun getCompanyLabel(
        @Header("Authorization") bearerToken: String
    ):List<LabelSettingsResponse>


    @GET("Category/sync")
    suspend fun getCategory(
        @Header("Authorization") bearerToken: String
    ): CategoryResponse

    @GET("Ticket/sync")
    suspend fun getTicket(
        @Header("Authorization") bearerToken: String
    ): TicketResponse

    @POST("orders/create")
    suspend fun postTicket(
        @Header("Authorization") bearerToken: String,
        @Body request: TicketPaymentRequest
    ): OrderResponse


}