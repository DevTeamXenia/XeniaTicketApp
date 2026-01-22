package com.example.ticket.data.room.entity


import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "CompanyLabel")
data class LabelSettings(

    @PrimaryKey
    val id: Int,

    val companyId: Int,
    val settingKey: String,
    val displayName: String,
    val displayNameMa: String?,
    val displayNameTa: String?,
    val displayNameTe: String?,
    val displayNameKa: String?,
    val displayNameHi: String?,
    val displayNameMr: String?,
    val displayNamePa: String?,
    val displayNameSi: String?,
    val createdBy: Int,
    val createdOn: String,
    val modifiedBy: Int?,
    val modifiedOn: String?,
    val active: Boolean
)

