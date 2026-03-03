package com.xenia.ticket.data.network.model

data class ReceiptLine(
    val text: String,
    val isBold: Boolean = false,
    val isCenter: Boolean = false
)