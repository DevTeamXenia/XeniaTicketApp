package com.xenia.ticket.data.network.model

import com.google.gson.annotations.SerializedName



data class CompanyResponse(

    @SerializedName("KeyCode")
    val keyCode: String,

    @SerializedName("Value")
    val value: String?,

    @SerializedName("PaymentConfig")
    val paymentConfig: PaymentConfig?
)

data class PaymentConfig(

    @SerializedName("ApplicationId")
    val applicationId: String
)


