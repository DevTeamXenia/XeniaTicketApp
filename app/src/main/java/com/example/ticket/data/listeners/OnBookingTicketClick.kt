package com.example.ticket.data.listeners

import com.example.ticket.data.network.model.TicketDto



interface OnBookingTicketClick {
    fun onTicketMinusClick(ticketItem: TicketDto)
    fun onTicketPlusClick(ticketItem: TicketDto)
}