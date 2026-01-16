package com.example.ticket.data.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Category")
data class Category(
    @PrimaryKey val categoryId: Int,
    val categoryName: String,
    val categoryNameMa: String?,
    val categoryNameTa: String?,
    val categoryNameTe: String?,
    val categoryNameKa: String?,
    val categoryNameHi: String?,
    val categoryNameMr: String?,
    val categoryNamePa: String?,
    val categoryNameSi: String?,
    val CategoryCompanyId: Int,
    val categoryCreatedDate: String,
    val categoryCreatedBy: Int,
    val categoryModifiedDate: String,
    val categoryModifiedBy: Int,
    val categoryActive: Boolean
)

