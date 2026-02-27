package com.xenia.ticket.data.network.model
data class TransactionReportResponse(
    val PageIndex: Int,
    val PageSize: Int,
    val TotalRecords: Int,
    val TotalPages: Int,
    val TotalAmount: Double,
    val Items: List<TransactionItem>
)
