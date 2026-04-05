package com.xenia.ticket.data.network.model

import com.google.gson.annotations.SerializedName

data class OrderResponse(

    @SerializedName("Status")
    val status: Boolean?,

    @SerializedName("Message")
    val message: String?,

    @SerializedName("ticket")
    val ticket: String?,

    @SerializedName("receipt")
    val receipt: String?
)