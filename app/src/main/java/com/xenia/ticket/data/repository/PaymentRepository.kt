package com.xenia.ticket.data.repository

import com.xenia.ticket.data.network.model.GenerateQrRequest
import com.xenia.ticket.data.network.model.GenerateQrResponse
import com.xenia.ticket.data.network.model.OrderResponse
import com.xenia.ticket.data.network.model.PaymentStatusResponse
import com.xenia.ticket.data.network.model.TicketPaymentRequest

import com.xenia.ticket.data.network.service.ApiClient.apiService


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