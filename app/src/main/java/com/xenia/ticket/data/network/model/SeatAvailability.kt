package com.xenia.ticket.data.network.model

data class SeatAvailability(
    val SeatId: Int,
    val ScreenId: Int,
    val RowName: String,
    val SeatNumber: Int,
    val SeatLabel: String,
    val Price: Double,
    val SortOrder: Int,
    val IsBooked: Boolean,
    val Status: Boolean
)