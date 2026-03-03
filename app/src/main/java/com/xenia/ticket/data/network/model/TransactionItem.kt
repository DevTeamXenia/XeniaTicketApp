package com.xenia.ticket.data.network.model

data class TransactionItem(
    val TicketId: Int,
    val TicketName: String,
    val TicketNameMa: String,
    val TicketNameHi: String,
    val TicketNameTa: String,
    val TicketNameKa: String,
    val TicketNameTe: String,
    val TicketNameSi: String,
    val TicketNamePa: String,
    val TicketNameMr: String,
    val Quantity: Int,
    val Rate: Double,
    val Amount: Double,
    val GenerateDate: String,
    val Status: String
)