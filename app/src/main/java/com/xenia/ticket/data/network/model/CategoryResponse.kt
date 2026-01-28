package com.xenia.ticket.data.network.model

import com.google.gson.annotations.SerializedName

data class CategoryResponse(
    @SerializedName("Status")
    val status: String,

    @SerializedName("Data")
    val data: List<CategoryItem>
)

data class CategoryItem(
    @SerializedName("CategoryId")
    val categoryId: Int,

    @SerializedName("CategoryName")
    val categoryName: String,

    @SerializedName("CategoryNameMa")
    val categoryNameMa: String?,

    @SerializedName("CategoryNameTa")
    val categoryNameTa: String?,

    @SerializedName("CategoryNameTe")
    val categoryNameTe: String?,

    @SerializedName("CategoryNameKa")
    val categoryNameKa: String?,

    @SerializedName("CategoryNameHi")
    val categoryNameHi: String?,

    @SerializedName("CategoryNameMr")
    val categoryNameMr: String?,

    @SerializedName("CategoryNamePa")
    val categoryNamePa: String?,

    @SerializedName("CategoryNameSi")
    val categoryNameSi: String?,

    @SerializedName("CategoryCompanyId")
    val categoryCompanyId: Int,

    @SerializedName("CategoryCreatedDate")
    val categoryCreatedDate: String,

    @SerializedName("CategoryCreatedBy")
    val categoryCreatedBy: Int,

    @SerializedName("CategoryModifiedDate")
    val categoryModifiedDate: String,

    @SerializedName("CategoryModifiedBy")
    val categoryModifiedBy: Int,

    @SerializedName("CatgeoryActive")
    val categoryActive: Boolean
)

