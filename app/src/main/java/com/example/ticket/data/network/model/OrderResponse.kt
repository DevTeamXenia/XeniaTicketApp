package com.example.ticket.data.network.model

import com.google.gson.annotations.SerializedName

data class OrderResponse(
    @SerializedName("Status")
    val status: String,
    @SerializedName("OrderId")
    val orderId: String,
)
