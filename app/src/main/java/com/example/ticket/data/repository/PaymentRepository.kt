package com.example.ticket.data.repository

import com.example.ticket.data.network.model.OrderResponse
import com.example.ticket.data.network.model.TicketPaymentRequest

import com.example.ticket.data.network.service.ApiClient


class PaymentRepository {

    suspend fun postTicket(
        bearerToken: String,
        request: TicketPaymentRequest
    ): OrderResponse {
        return ApiClient.apiService.postTicket(bearerToken, request)
    }

}