package com.xenia.ticket.data.network.model

data class GenerateQrRequest(
    val Amount: Int,
    val name: String,
    val phone: String
)
