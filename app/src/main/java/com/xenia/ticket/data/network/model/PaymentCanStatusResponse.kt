package com.xenia.ticket.data.network.model

data class PaymentCanStatusResponse(
    val success: Boolean,
    val data: PaymentData
)

data class PaymentData(
    val pspRefNo: String?,
    val status: String?,
    val statusDesc: String?,
    val customerName: String?,
    val respCode: String?,
    val respMessage: String?,
    val upiTxnId: String?,
    val txnTime: String?,
    val amount: String?,
    val upiId: String?,
    val requestTime: String?,
    val custRefNo: String?,
    val remark: String?,
    val payeeVpa: String?
)