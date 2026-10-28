package com.xenia.ticket.data.repository

import android.util.Log
import com.xenia.ticket.data.network.model.FedQrRequest
import com.xenia.ticket.data.network.model.FedQrResponse
import com.xenia.ticket.data.network.model.OrderResponse
import com.xenia.ticket.data.network.model.PaymentCanStatusResponse
import com.xenia.ticket.data.network.model.PaymentStatusResponse
import com.xenia.ticket.data.network.model.QrCanaraResponse
import com.xenia.ticket.data.network.model.SibPaymentStatusResponse
import com.xenia.ticket.data.network.model.QrRequest
import com.xenia.ticket.data.network.model.QrResponse
import com.xenia.ticket.data.network.model.StatusRequest
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
//    suspend fun postTicket(
//        bearerToken: String,
//        request: TicketPaymentRequest
//    ): OrderResponse {
//        return apiService.postTicket(bearerToken, request)
//    }

    suspend fun postTicket(
        bearerToken: String,
        request: TicketPaymentRequest
    ): OrderResponse {

        Log.d(
            "PAYMENT_FLOW",
            "postTicket START | paymentStatus=${request.tPaymentStatus} | " +
                    "paymentMode=${request.tPaymentMode} | " +
                    "itemsCount=${request.Items.size}"
        )

        try {

            Log.d(
                "PAYMENT_FLOW",
                "Calling postTicket API..."
            )

            val response = apiService.postTicket(
                bearerToken,
                request
            )

            Log.d(
                "PAYMENT_FLOW",
                "postTicket SUCCESS | response=$response"
            )

            return response

        } catch (e: Exception) {

            Log.e(
                "PAYMENT_FLOW",
                "postTicket FAILED | " +
                        "errorType=${e.javaClass.simpleName} | " +
                        "message=${e.message}",
                e
            )

            throw e
        }
    }
//    suspend fun getFedPaymentStatus(
//        orderId: String,
//        token: String
//    ): PaymentStatusResponse {
//        return apiService.getFedPaymentStatus(orderId, token)
//    }
suspend fun getFedPaymentStatus(
    orderId: String,
    token: String
): PaymentStatusResponse {

    Log.d(
        "FedPayment",
        "API CALL START | orderId=$orderId"
    )

    try {

        Log.d(
            "FedPayment",
            "Calling getFedPaymentStatus API..."
        )

        val response = apiService.getFedPaymentStatus(
            orderId,
            token
        )

        Log.d(
            "FedPayment",
            "API CALL SUCCESS | response=$response"
        )

        return response

    } catch (e: Exception) {

        Log.e(
            "FedPayment",
            "API CALL FAILED | orderId=$orderId | " +
                    "error=${e.javaClass.simpleName} | message=${e.message}",
            e
        )

        throw e
    }
}
    suspend fun generateSibQr(
        token: String,
        payFor: String,
        request: QrRequest
    ): QrResponse {

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
            request = StatusRequest(pspRefNo = orderId),
            token = token
        )
    }


    suspend fun generateCanaraQr(
        token: String,
        payFor: String,
        request: QrRequest
    ): QrCanaraResponse {

        return apiService.generateCanaraQr(
            token = token,
            payFor = payFor,
            request = request
        )
    }


//    suspend fun getCanaraPaymentStatus(
//        orderId: String,
//        token: String
//    ): PaymentCanStatusResponse {
//
//        return apiService.getCanaraPaymentStatus(
//            payFor = "Common",
//            request = StatusRequest(pspRefNo = orderId),
//            token = token
//        )
//    }

    suspend fun getCanaraPaymentStatus(
        orderId: String,
        token: String
    ): PaymentCanStatusResponse {

        Log.d("CanaraPayment", "================================")
        Log.d("CanaraPayment", "API CALL START")
        Log.d("CanaraPayment", "orderId=$orderId")
        Log.d("CanaraPayment", "payFor=Common")

        try {

            val response = apiService.getCanaraPaymentStatus(
                payFor = "Common",
                request = StatusRequest(pspRefNo = orderId),
                token = token
            )

            Log.d("CanaraPayment", "API CALL SUCCESS")
            Log.d("CanaraPayment", "response=$response")

            return response

        } catch (e: retrofit2.HttpException) {

            Log.e(
                "CanaraPayment",
                "HTTP ERROR | code=${e.code()} | message=${e.message()}",
                e
            )

            try {
                Log.e(
                    "CanaraPayment",
                    "ERROR BODY=${e.response()?.errorBody()?.string()}"
                )
            } catch (bodyException: Exception) {
                Log.e(
                    "CanaraPayment",
                    "Could not read error body",
                    bodyException
                )
            }

            throw e

        } catch (e: java.net.SocketTimeoutException) {

            Log.e(
                "CanaraPayment",
                "TIMEOUT ERROR | API did not respond in time",
                e
            )

            throw e

        } catch (e: java.net.UnknownHostException) {

            Log.e(
                "CanaraPayment",
                "NETWORK ERROR | Cannot connect to server/host",
                e
            )

            throw e

        } catch (e: Exception) {

            Log.e(
                "CanaraPayment",
                "UNKNOWN API ERROR | ${e.javaClass.simpleName} | ${e.message}",
                e
            )

            throw e
        }
    }



}