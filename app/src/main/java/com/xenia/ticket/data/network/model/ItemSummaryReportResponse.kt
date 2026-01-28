package com.xenia.ticket.data.network.model

import com.google.gson.annotations.SerializedName

data class ItemSummaryReportResponse(
    val status: String,
    val summary: Summary,

    @SerializedName("DarshanTickets")
    val darshanTickets: List<OfferItem> = emptyList()
)

data class Summary(
    @SerializedName("DarshanTickets")
    val darshanTickets: CategorySummary,
    val GrandTotalAmountAll: Double
)

data class CategorySummary(
    val TotalCount: Int,
    val GrandTotalQty: Int,
    val GrandTotalAmount: Double
)

data class OfferItem(
    @SerializedName("OfferId")
    val offerId: Int? = null,

    @SerializedName("OfferName")
    val offerName: String? = null,

    @SerializedName("TicketId")
    val ticketId: Int? = null,

    @SerializedName("TicketName")
    val ticketName: String? = null,

    @SerializedName("OfferNameMa")
    val offerNameMa: String? = null,

    @SerializedName("OfferNameTa")
    val offerNameTa: String? = null,

    @SerializedName("OfferNameTe")
    val offerNameTe: String? = null,

    @SerializedName("OfferNameHi")
    val offerNameHi: String? = null,

    @SerializedName("OfferNameKn")
    val offerNameKn: String? = null,

    @SerializedName("TotalQty")
    val totalQty: Double = 0.0,

    @SerializedName("Rate")
    val rate: Double = 0.0,

    @SerializedName("TotalAmount")
    val totalAmount: Double = 0.0
)
