package com.xenia.ticket.data.listeners

import com.xenia.ticket.data.network.model.TicketDto

interface OnTicketClickListener {
    fun onTicketClick(ticketItem: TicketDto)
    fun onTicketClear(ticketItem: TicketDto)
    fun onTicketAdded()
}