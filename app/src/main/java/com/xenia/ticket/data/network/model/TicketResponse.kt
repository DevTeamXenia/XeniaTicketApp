package com.xenia.ticket.data.network.model



import com.google.gson.annotations.SerializedName

data class TicketResponse(
    @SerializedName("Status")
    val status: String,

    @SerializedName("Data")
    val data: List<TicketDto>
)

data class TicketDto(
    @SerializedName("TicketId")
    val ticketId: Int,

    @SerializedName("TicketName")
    val ticketName: String,

    @SerializedName("TicketNameMa")
    val ticketNameMa: String?,

    @SerializedName("TicketNameTa")
    val ticketNameTa: String?,

    @SerializedName("TicketNameTe")
    val ticketNameTe: String?,

    @SerializedName("TicketNameKa")
    val ticketNameKa: String?,

    @SerializedName("TicketNameHi")
    val ticketNameHi: String?,

    @SerializedName("TicketNamePa")
    val ticketNamePa: String?,

    @SerializedName("TicketNameMr")
    val ticketNameMr: String?,

    @SerializedName("TicketNameSi")
    val ticketNameSi: String?,

    @SerializedName("TicketCategoryId")
    val ticketCategoryId: Int,

    @SerializedName("TicketCompanyId")
    val ticketCompanyId: Int,

    @SerializedName("TicketAmount")
    val ticketAmount: Double,

    @SerializedName("TicketCreatedDate")
    val ticketCreatedDate: String,

    @SerializedName("TicketCreatedBy")
    val ticketCreatedBy: Int,

    @SerializedName("TicketActive")
    val ticketActive: Boolean
)
