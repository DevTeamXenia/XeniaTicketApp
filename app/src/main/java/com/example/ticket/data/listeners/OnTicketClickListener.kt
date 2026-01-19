package com.example.ticket.data.listeners

import com.example.ticket.data.network.model.TicketDto
import com.example.ticket.data.room.entity.ActiveTicket
import com.example.ticket.data.room.entity.Ticket

interface OnTicketClickListener {
    fun onTicketClick(ticketItem: TicketDto)
    fun onTicketClear(ticketItem: TicketDto)
    fun onTicketAdded()
}