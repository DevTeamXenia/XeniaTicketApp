package com.example.ticket.data.listeners

import com.example.ticket.data.room.entity.Ticket

interface OnTicketClickListener {
    fun onDarshanClick(darshanItem: Ticket)
    fun onDarshanClear(darshanItem: Ticket)
    fun onDarshanAdded()
}