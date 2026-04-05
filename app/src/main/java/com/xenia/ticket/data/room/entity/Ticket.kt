package com.xenia.ticket.data.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "Ticket")
data class Ticket(
    @PrimaryKey
    val id: Int,
    val name: String,
    val nameMa: String?,
    val nameTa: String?,
    val nameTe: String?,
    val nameKa: String?,
    val nameHi: String?,
    val namePa: String?,
    val nameMr: String?,
    val nameSi: String?,
    val categoryId: Int,
    val companyId: Int,
    val amount: Double,
    val createdDate: String,
    val createdBy: Int,
    val active: Boolean,
    val combo: Boolean,
)