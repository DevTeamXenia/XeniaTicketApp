package com.xenia.ticket.data.network.model

import com.google.gson.annotations.SerializedName

data class FedQrResponse(
    @SerializedName("OrderId")
    val OrderId: String?,

    @SerializedName("UpiIntentUrl")
    val UpiIntentUrl: String?
)

