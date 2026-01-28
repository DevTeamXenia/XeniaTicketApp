package com.xenia.ticket.data.network.service

import com.xenia.ticket.data.network.model.CategoryResponse
import com.xenia.ticket.data.network.model.CompanyResponse
import com.xenia.ticket.data.network.model.GenerateQrRequest
import com.xenia.ticket.data.network.model.GenerateQrResponse
import com.xenia.ticket.data.network.model.ItemSummaryReportResponse
import com.xenia.ticket.data.network.model.LabelSettingsResponse
import com.xenia.ticket.data.network.model.LoginRequest
import com.xenia.ticket.data.network.model.LoginResponse
import com.xenia.ticket.data.network.model.LogoutResponse
import com.xenia.ticket.data.network.model.OrderResponse
import com.xenia.ticket.data.network.model.PaymentStatusResponse
import com.xenia.ticket.data.network.model.SummaryReportResponse
import com.xenia.ticket.data.network.model.TicketPaymentRequest
import com.xenia.ticket.data.network.model.TicketResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
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
    ): List<LabelSettingsResponse>


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

    @GET("Report/SummaryReport")
    suspend fun getSummaryReport(
        @Header("Authorization") bearerToken: String,
        @Query("startDateTime") startDate: String,
        @Query("endDateTime") endDate: String,
    ): SummaryReportResponse

    @GET("Report/OfferingsSummary")
    suspend fun getItemSummaryReport(
        @Header("Authorization") bearerToken: String,
        @Query("startDateTime") startDateTime: String,
        @Query("endDateTime") endDateTime: String
    ): ItemSummaryReportResponse

    @POST("payments/fed/generateQr")
    suspend fun generateQr(
        @Header("Authorization") token: String,
        @Body request: GenerateQrRequest
    ): GenerateQrResponse



    @GET("payments/fed/status/{orderId}")
    suspend fun getFedPaymentStatus(
        @Path("orderId") orderId: String,
        @Header("Authorization") token: String
    ): PaymentStatusResponse

}