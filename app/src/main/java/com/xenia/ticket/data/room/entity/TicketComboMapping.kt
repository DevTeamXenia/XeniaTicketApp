package com.xenia.ticket.data.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "TicketComboMapping")
data class TicketComboMapping(
    @PrimaryKey val id: Int,
    val parentTicketId: Int,
    val childTicketId: Int,
    val childTicketType: String?,
    val createdDate: String?
)