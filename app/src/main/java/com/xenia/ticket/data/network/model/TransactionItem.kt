package com.xenia.ticket.data.network.model

data class TransactionItem(
    val OrderId: Int,
    val TransactionId: String?,
    val ReceiptNo: String?,
    val TicketNo: String,
    val TransactionDate: String,
    val CustomerName: String?,
    val PhoneNumber: String?,
    val Amount: Double,
    val SourceType: String,
    val Status: String
)