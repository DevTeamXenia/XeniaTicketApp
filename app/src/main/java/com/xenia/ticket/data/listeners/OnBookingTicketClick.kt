package com.xenia.ticket.data.listeners

import com.xenia.ticket.data.network.model.Tickets



interface OnBookingTicketClick {
    fun onTicketMinusClick(ticketItem: Tickets)
    fun onTicketPlusClick(ticketItem: Tickets)
}