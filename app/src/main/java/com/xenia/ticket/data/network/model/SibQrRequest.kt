package com.xenia.ticket.data.network.model

data class SibQrRequest(
    val transactionReferenceID: String,
    val amount: String,
    val name: String,
    val phoneNumber: String
)

