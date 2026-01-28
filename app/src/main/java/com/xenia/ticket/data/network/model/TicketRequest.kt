package com.xenia.ticket.data.network.model

data class TicketRequest(
val dTranscationId: String,
val dName: String,
val dPhoneNumber: String,
val dIdProof: String,
val dIdProofNo: String,
val dImage: String,
val dTotalAmount: Double,
val dPaymentStatus: String,
val dPaymentDes: String,
val dProccedStatus: String,
val TK_DarshanDetails: List<TicketDetails>
)
