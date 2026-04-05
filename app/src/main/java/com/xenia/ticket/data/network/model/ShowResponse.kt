package com.xenia.ticket.data.network.model



import com.google.gson.annotations.SerializedName

data class ShowResponse(
    @SerializedName("Status")
    val status: String,

    @SerializedName("Data")
    val data: List<ShowDto>
)

data class ShowDto(
    @SerializedName("ShowId")
    val showId: Int,

    @SerializedName("ShowName")
    val showName: String,

    @SerializedName("ShowNameMa")
    val showNameMa: String?,

    @SerializedName("ShowNameTa")
    val showNameTa: String?,

    @SerializedName("ShowNameTe")
    val showNameTe: String?,

    @SerializedName("ShowNameKa")
    val showNameKa: String?,

    @SerializedName("ShowNameHi")
    val showNameHi: String?,

    @SerializedName("ShowNamePa")
    val showNamePa: String?,

    @SerializedName("ShowNameMr")
    val showNameMr: String?,

    @SerializedName("ShowNameSi")
    val showNameSi: String?,

    @SerializedName("Description")
    val description: String?,

    @SerializedName("DurationMinutes")
    val durationMinutes: Int,

    @SerializedName("Amount")
    val amount: Double,

    @SerializedName("CompanyId")
    val companyId: Int,

    @SerializedName("CreatedDate")
    val createdDate: String,

    @SerializedName("CreatedBy")
    val createdBy: Int,

    @SerializedName("IsActive")
    val isActive: Boolean,

)
