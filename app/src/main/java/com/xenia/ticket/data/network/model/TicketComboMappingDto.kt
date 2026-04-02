package com.xenia.ticket.data.network.model

import com.google.gson.annotations.SerializedName

data class TicketComboMappingDto(
    @SerializedName("Id")
    val id: Int,

    @SerializedName("ParentTicketId")
    val parentTicketId: Int,

    @SerializedName("ChildTicketId")
    val childTicketId: Int,

    @SerializedName("CreatedDate")
    val createdDate: String?
)
