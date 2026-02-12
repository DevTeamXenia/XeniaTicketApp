package com.xenia.ticket.data.repository

import com.xenia.ticket.data.network.model.FedQrRequest
import com.xenia.ticket.data.network.model.FedQrResponse
import com.xenia.ticket.data.network.model.OrderResponse
import com.xenia.ticket.data.network.model.PaymentStatusResponse
import com.xenia.ticket.data.network.model.SibPaymentStatusResponse
import com.xenia.ticket.data.network.model.SibQrRequest
import com.xenia.ticket.data.network.model.SibQrResponse
import com.xenia.ticket.data.network.model.SibStatusRequest
import com.xenia.ticket.data.network.model.TicketPaymentRequest

import com.xenia.ticket.data.network.service.ApiClient.apiService


class PaymentRepository {

    suspend fun generateFedQr(
        token: String,
        request: FedQrRequest
    ): FedQrResponse {
        return apiService.generateFedQr(
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
    suspend fun generateSibQr(
        token: String,
        payFor: String,
        request: SibQrRequest
    ): SibQrResponse {

        return apiService.generateSibQr(
            token = token,
            payFor = payFor,
            request = request
        )
    }
    suspend fun getSibPaymentStatus(
        orderId: String,
        token: String
    ): SibPaymentStatusResponse {

        return apiService.getSibPaymentStatus(
            payFor = "Common",
            request = SibStatusRequest(pspRefNo = orderId),
            token = token
        )
    }



}