package com.xenia.ticket.data.network.model

import com.google.gson.annotations.SerializedName

data class OrderResponse(

    @SerializedName("Status")
    val status: String?,

    @SerializedName("ticket")
    val ticket: String?,

    @SerializedName("receipt")
    val receipt: String?
)