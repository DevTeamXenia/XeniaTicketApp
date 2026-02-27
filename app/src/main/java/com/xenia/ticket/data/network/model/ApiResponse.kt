package com.xenia.ticket.data.network.model

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(

    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("data")
    val data: T? = null
)
