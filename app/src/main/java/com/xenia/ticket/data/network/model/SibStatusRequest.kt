package com.xenia.ticket.data.network.model

import com.google.gson.annotations.SerializedName

data class SibStatusRequest(
    @SerializedName("pspRefNo")
    val pspRefNo: String
)

