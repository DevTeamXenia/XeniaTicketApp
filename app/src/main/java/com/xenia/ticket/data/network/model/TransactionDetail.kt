package com.xenia.ticket.data.network.model

data class TransactionDetail(
    val TicketId: Int,
    val Quantity: Int,
    val Rate: Double,
    val Amount: Double,
    val GenerateDate: String,
    val Status: String
)