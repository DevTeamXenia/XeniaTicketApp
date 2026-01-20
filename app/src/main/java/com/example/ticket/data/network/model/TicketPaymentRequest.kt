package com.example.ticket.data.network.model

import com.google.gson.annotations.SerializedName

data class TicketPaymentRequest(
    @SerializedName("CompanyId") val CompanyId: Int,
    @SerializedName("UserId") val UserId: Int,
    @SerializedName("Name") val Name: String,
    @SerializedName("tTranscationId") val tTranscationId: String,
    @SerializedName("tCustRefNo") val tCustRefNo: String,
    @SerializedName("tNpciTransId") val tNpciTransId: String,
    @SerializedName("tIdProofNo") val tIdProofNo: String,
    @SerializedName("tImage") val tImage: String,
    @SerializedName("PhoneNumber") val PhoneNumber: String,
    @SerializedName("tPaymentStatus") val tPaymentStatus: String,
    @SerializedName("tPaymentMode") val tPaymentMode: String,
    @SerializedName("tPaymentDes") val tPaymentDes: String,
    @SerializedName("Items") val Items: List<Item>
)
 {
    data class Item(
        val taCategoryId: Int,
        val TicketId: Int,
        val Quantity: Int,
        val Rate: Double
    )
}

