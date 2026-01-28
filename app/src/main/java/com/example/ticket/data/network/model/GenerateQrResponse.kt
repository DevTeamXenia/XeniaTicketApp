package com.example.ticket.data.network.model

import com.google.gson.annotations.SerializedName

data class GenerateQrResponse(
    @SerializedName("OrderId")
    val OrderId: String?,

    @SerializedName("UpiIntentUrl")
    val UpiIntentUrl: String?
)

