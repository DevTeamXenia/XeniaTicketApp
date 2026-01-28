package com.example.ticket.data.repository

import android.util.Log
import com.example.ticket.data.network.model.ItemSummaryReportResponse
import com.example.ticket.data.network.model.SummaryReportResponse
import com.example.ticket.data.network.service.ApiClient.apiService
import com.example.ticket.utils.common.SessionManager
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
}
