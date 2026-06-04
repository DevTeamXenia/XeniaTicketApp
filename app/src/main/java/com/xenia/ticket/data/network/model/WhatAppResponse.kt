package com.xenia.ticket.data.network.model

import com.google.gson.annotations.SerializedName

data class WhatAppResponse(
    @SerializedName("Message")
    val message: String = ""
)
