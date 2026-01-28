package com.example.ticket.data.network.model

data class SummaryReportResponse(
    val TotalOrderAmount: Number,
    val TotalVazhipaduOfferingsAmount: Number,
    val TotalPoojaItemAmount: Number,
    val TotalDarshanAmount: Number,
    val TotalOrderCount: Int,
    val TotalVazhipaduOfferingsCount: Int,
    val TotalPoojaItemCount: Int,
    val TotalDarshanCount: Int,
    val TotalCash: Number,
    val TotalUpi: Number,
    val TotalCard: Number,
    val TotalAmount: Number
)
