package com.xenia.ticket.data.network.model

import com.google.gson.annotations.SerializedName

data class SibPaymentStatusResponse(

    @SerializedName("PspRefNo")
    val pspRefNo: String?,

    @SerializedName("Status")
    val Status: String?,

    @SerializedName("StatusDesc")
    val statusDesc: String?,

    @SerializedName("TxnAuthDate")
    val txnAuthDate: String?
)


