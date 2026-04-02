package com.xenia.ticket.data.listeners

import com.xenia.ticket.data.network.model.ActiveItem
import com.xenia.ticket.data.network.model.TicketDto

interface OnTicketClickListener {
    fun onTicketClick(item: ActiveItem)
    fun onTicketClear(item: ActiveItem)
    fun onTicketAdded(ticketId: Int)
}