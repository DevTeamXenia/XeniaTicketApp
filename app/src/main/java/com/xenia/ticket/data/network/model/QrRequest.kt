package com.xenia.ticket.data.network.model

data class QrRequest(
    val transactionReferenceID: String,
    val amount: String,
    val name: String,
    val phoneNumber: String
)

