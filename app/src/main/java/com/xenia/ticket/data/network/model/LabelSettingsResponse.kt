package com.xenia.ticket.data.network.model


import com.google.gson.annotations.SerializedName

data class LabelSettingsResponse(

    @SerializedName("Id")
    val id: Int,

    @SerializedName("CompanyId")
    val companyId: Int,

    @SerializedName("SettingKey")
    val settingKey: String,

    @SerializedName("DisplayName")
    val displayName: String,

    @SerializedName("DisplayNameMa")
    val displayNameMa: String?,

    @SerializedName("DisplayNameTa")
    val displayNameTa: String?,

    @SerializedName("DisplayNameTe")
    val displayNameTe: String?,

    @SerializedName("DisplayNameKa")
    val displayNameKa: String?,

    @SerializedName("DisplayNameHi")
    val displayNameHi: String?,

    @SerializedName("DisplayNameMr")
    val displayNameMr: String?,

    @SerializedName("DisplayNamePa")
    val displayNamePa: String?,

    @SerializedName("DisplayNameSi")
    val displayNameSi: String?,

    @SerializedName("CreatedBy")
    val createdBy: Int,

    @SerializedName("CreatedOn")
    val createdOn: String,

    @SerializedName("ModifiedBy")
    val modifiedBy: Int?,

    @SerializedName("ModifiedOn")
    val modifiedOn: String?,

    @SerializedName("Active")
    val active: Boolean
)


