package com.xenia.ticket.data.network.service

import com.xenia.ticket.data.network.model.CategoryResponse
import com.xenia.ticket.data.network.model.CompanyResponse
import com.xenia.ticket.data.network.model.FedQrRequest
import com.xenia.ticket.data.network.model.FedQrResponse
import com.xenia.ticket.data.network.model.ItemSummaryReportResponse
import com.xenia.ticket.data.network.model.LabelSettingsResponse
import com.xenia.ticket.data.network.model.LoginRequest
import com.xenia.ticket.data.network.model.LoginResponse
import com.xenia.ticket.data.network.model.LogoutResponse
import com.xenia.ticket.data.network.model.OrderResponse
import com.xenia.ticket.data.network.model.SibQrRequest
import com.xenia.ticket.data.network.model.PaymentStatusResponse
import com.xenia.ticket.data.network.model.PineLabGenerateRequest
import com.xenia.ticket.data.network.model.PineLabGenerateResponse
import com.xenia.ticket.data.network.model.SibPaymentStatusResponse
import com.xenia.ticket.data.network.model.SibQrResponse
import com.xenia.ticket.data.network.model.SibStatusRequest
import com.xenia.ticket.data.network.model.SummaryReportResponse
import com.xenia.ticket.data.network.model.TicketPaymentRequest
import com.xenia.ticket.data.network.model.TicketResponse
import com.xenia.ticket.data.network.model.TransactionDetailItem
import com.xenia.ticket.data.network.model.TransactionItem
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.Response

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

    @GET("Report/TransactionSummaryReport")
    suspend fun getTransactionDetailReport(
        @Header("Authorization") authorization: String,
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String,
        @Query("userId") userId: Int,
    ): Response<List<TransactionDetailItem>>

    @GET("Report/TransactionDetailReport")
    suspend fun getTransactionDetails(
        @Header("Authorization") token: String,
        @Query("orderId") orderId: Int,
    ): List<TransactionItem>

    @POST("payments/fed/generateQr")
    suspend fun generateFedQr(
        @Header("Authorization") token: String,
        @Body request: FedQrRequest
    ): FedQrResponse
    @GET("payments/fed/status/{orderId}")
    suspend fun getFedPaymentStatus(
        @Path("orderId") orderId: String,
        @Header("Authorization") token: String
    ): PaymentStatusResponse
    @POST("payments/sib/qr")
    suspend fun generateSibQr(
        @Header("Authorization") token: String,
        @Query("payFor") payFor: String,
        @Body request: SibQrRequest
    ): SibQrResponse

    @POST("payments/sib/status")
    suspend fun getSibPaymentStatus(
        @Query("payFor") payFor: String = "Common",
        @Body request: SibStatusRequest,
        @Header("Authorization") token: String
    ): SibPaymentStatusResponse

    @POST("payments/pineLab/generate")
    suspend fun generatePineLabPayment(
        @Header("Authorization") token: String,
        @Body request: PineLabGenerateRequest
    ): Response<PineLabGenerateResponse>
}