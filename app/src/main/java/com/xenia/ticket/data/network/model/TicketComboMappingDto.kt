package com.xenia.ticket.data.network.model

data class TicketComboMappingDto(
    val id: Int,
    val parentTicketId: Int,
    val childTicketId: Int,
    val createdDate: String
)
