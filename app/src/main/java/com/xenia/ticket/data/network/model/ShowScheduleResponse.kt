package com.xenia.ticket.data.network.model

data class ShowScheduleResponse(
    val showTime: String,
    val screenName: String,
    val availableSeats: Int
)
