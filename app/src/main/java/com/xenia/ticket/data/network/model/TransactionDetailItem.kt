package com.xenia.ticket.data.network.model

data class TransactionDetailItem(
    val OrderId: Int,
    val TicketNo: String,
    val ReceiptNo: String,
    val TransactionId: String?,
    val CustomerName: String?,
    val PhoneNumber: String?,
    val GeneratedDate: String,
    val TotalAmount: Double,
    val PaymentMode: String,
    val PaymentStatus: String,
    val UserId: Int,
    val Details: List<TransactionDetail>
)