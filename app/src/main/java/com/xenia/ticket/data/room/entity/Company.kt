package com.xenia.ticket.data.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "CompanySettings")
data class Company(
    @PrimaryKey
    val companySettingsId: Int,
    val companyId: Int,
    val keyCode: String,
    val value: String,
    val active: Boolean
)



