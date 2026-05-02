package com.xenia.ticket.data.network.model

data class ShowScheduleResponse(
    val ScheduleId: Int,
    val ShowId: Int,
    val ScreenId: Int,
    val ShowDay: String,
    val StartTime: String,
    val EndTime: String,
    val ScreenName: String,
    val TotalSeats: Int,
    val BookedSeats: Int,
    val AvailableSeats: Int,
)
