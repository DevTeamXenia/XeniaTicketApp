package com.example.ticket.data.repository

import com.example.ticket.data.network.model.GenerateQrRequest
import com.example.ticket.data.network.model.GenerateQrResponse
import com.example.ticket.data.network.model.OrderResponse
import com.example.ticket.data.network.model.PaymentStatusResponse
import com.example.ticket.data.network.model.TicketPaymentRequest

import com.example.ticket.data.network.service.ApiClient
import com.example.ticket.data.network.service.ApiClient.apiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class PaymentRepository {

    suspend fun generateQr(
        token: String,
        request: GenerateQrRequest
    ): GenerateQrResponse {
        return apiService.generateQr(
            token = token,
            request = request
        )
    }
    suspend fun postTicket(
        bearerToken: String,
        request: TicketPaymentRequest
    ): OrderResponse {
        return apiService.postTicket(bearerToken, request)
    }
    suspend fun getFedPaymentStatus(
        orderId: String,
        token: String
    ): PaymentStatusResponse {
        return apiService.getFedPaymentStatus(orderId, token)
    }

}