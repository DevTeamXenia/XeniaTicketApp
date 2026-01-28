package com.xenia.ticket.data.network.model

import com.google.gson.annotations.SerializedName

data class CompanyResponse(
    @SerializedName("CompanySettingsId")
    val companySettingsId: Int,

    @SerializedName("CompanyId")
    val companyId: Int,

    @SerializedName("KeyCode")
    val keyCode: String,

    @SerializedName("Value")
    val value: String?,

    @SerializedName("Active")
    val active: Boolean
)

