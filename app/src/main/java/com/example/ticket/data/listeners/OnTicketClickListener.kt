package com.example.ticket.data.listeners

import com.example.ticket.data.room.entity.ActiveTicket
import com.example.ticket.data.room.entity.Ticket

interface OnTicketClickListener {
    fun onTicketClick(darshanItem: ActiveTicket)
    fun onTicketClear(darshanItem: ActiveTicket)
    fun onTicketAdded()
}