package com.xenia.ticket.data.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Show")
data class Show(
    @PrimaryKey
    val showId: Int,
    val showName: String,
    val showNameMa: String?,
    val showNameTa: String?,
    val showNameTe: String?,
    val showNameKa: String?,
    val showNameHi: String?,
    val showNamePa: String?,
    val showNameMr: String?,
    val description: String?,
    val showNameSi: String?,
    val durationMinutes: Int,
    val companyId: Int,
    val createdDate: String,
    val createdBy: Int,
    val isActive: Boolean,
)