package com.xenia.ticket.data.listeners

import com.xenia.ticket.data.network.model.TicketDto



interface OnBookingTicketClick {
    fun onTicketMinusClick(ticketItem: TicketDto)
    fun onTicketPlusClick(ticketItem: TicketDto)
}