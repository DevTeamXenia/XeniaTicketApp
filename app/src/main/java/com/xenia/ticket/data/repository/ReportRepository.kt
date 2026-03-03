package com.xenia.ticket.data.repository

import android.util.Log
import com.xenia.ticket.data.network.model.ItemSummaryReportResponse
import com.xenia.ticket.data.network.model.SummaryReportResponse
import com.xenia.ticket.data.network.model.TransactionDetailItem
import com.xenia.ticket.data.network.model.TransactionItem
import com.xenia.ticket.data.network.service.ApiClient.apiService
import com.xenia.ticket.utils.common.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.text.isBlank

class ReportRepository(private val sessionManager: SessionManager) {

    suspend fun getSummaryReport(
        token: String,
        startDateTime : String,
        endDateTime : String
    ): SummaryReportResponse = withContext(Dispatchers.IO) {

        apiService.getSummaryReport(
            bearerToken = token,
            startDate = startDateTime,
            endDate = endDateTime
        )
    }
    suspend fun getItemSummaryReport(
        token: String,
        startDateTime: String,
        endDateTime: String
    ): ItemSummaryReportResponse = withContext(Dispatchers.IO) {

        if (startDateTime.isBlank() || endDateTime.isBlank()) {
            throw kotlin.IllegalArgumentException("startDate or endDate is empty")
        }
        Log.d(
            "SUMMARY_API_REQUEST",
            "startDate=$startDateTime, endDate=$endDateTime"
        )
        apiService.getItemSummaryReport(
            bearerToken = token,
            startDateTime = startDateTime,
            endDateTime = endDateTime
        )
    }

    suspend fun fetchTransactionReport(
        startDate: String,
        endDate: String,
        userId: Int,
    ): List<TransactionDetailItem> {

        val token = sessionManager.getToken()
            ?: throw Exception("Token is null")

        val response = apiService.getTransactionDetailReport(
            authorization = token,
            startDate = startDate,
            endDate = endDate,
            userId = userId
        )

        if (!response.isSuccessful) {
            throw Exception("API Error ${response.code()}")
        }

        return response.body()
            ?: emptyList()
    }


    suspend fun fetchTransactionDetails(
        orderId: Int
    ): List<TransactionItem> {

        val token = sessionManager.getToken()
            ?: throw IllegalStateException("Token is null")

        return apiService.getTransactionDetails(
            orderId = orderId,
            token = "Bearer $token"
        )
    }

}
