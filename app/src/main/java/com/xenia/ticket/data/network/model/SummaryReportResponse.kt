package com.xenia.ticket.data.network.model

data class SummaryReportResponse(
    val TotalOrderAmount: Number,
    val TotalOrderCount: Int,
    val TotalCash: Number,
    val TotalUpi: Number,
    val TotalCard: Number,
    val TotalAmount: Number
)
