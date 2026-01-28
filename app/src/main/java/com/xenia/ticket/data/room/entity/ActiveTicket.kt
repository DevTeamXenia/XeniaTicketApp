package com.xenia.ticket.data.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "ActiveTickets")
data class ActiveTicket(
    @PrimaryKey
    val ticketId: Int,
    val ticketName: String,
    val ticketNameMa: String?,
    val ticketNameTa: String?,
    val ticketNameTe: String?,
    val ticketNameKa: String?,
    val ticketNameHi: String?,
    val ticketNamePa: String?,
    val ticketNameMr: String?,
    val ticketNameSi: String?,
    val ticketCategoryId: Int,
    val ticketCompanyId: Int,
    val ticketAmount: Double,
    val ticketCreatedDate: String,
    val ticketCreatedBy: Int,
    val ticketActive: Boolean
)