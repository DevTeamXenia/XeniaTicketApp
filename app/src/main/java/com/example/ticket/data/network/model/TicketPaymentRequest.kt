package com.example.ticket.data.network.model

data class TicketPaymentRequest(

val CompanyId: Int,
val UserId: Int,
val Name: String,
val tTranscationId: String,
val tCustRefNo: String,
val tNpciTransId: String,
val tIdProofNo: String,
val tImage: String,
val PhoneNumber: String,
val tPaymentStatus: String,
val tPaymentMode: String,
val tPaymentDes: String,
val Items: List<Item>

) {
    data class Item(
        val taCategoryId: Int,
        val TicketId: Int,
        val Quantity: Int,
        val Rate: Double
    )
}

