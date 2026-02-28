package com.xenia.ticket.data.network.model

data class PineLabGenerateRequest(
    val transcationId: String,
    val Amount: Int,
    val name: String,
    val phone: String
)
