package com.example.ticket.data.repository

import com.example.ticket.data.room.dao.TicketDao
import com.example.ticket.data.room.entity.Ticket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class TicketRepository(private val ticketDao: TicketDao) {


    suspend fun insertCartItem(ticket: Ticket) = withContext(Dispatchers.IO) {
        val ticketExists = ticketDao.getCartItemByTicketId(ticket.ticketId) != null

        if (ticketExists) {
            val existingTicket = ticketDao.getCartItemByTicketId(ticket.ticketId)!!
            val newQty = existingTicket.cartQty + ticket.cartQty
            val newAmount = existingTicket.cartTotalAmount + ticket.cartTotalAmount

            ticketDao.updateExistingTicket(
                ticketId = ticket.ticketId,
                newQty = newQty,
                additionalAmount = newAmount
            )
        } else {
            ticketDao.insertCartItem(ticket)
        }
    }

    suspend fun insertCartBookingItem(ticket: Ticket, key: String) = withContext(Dispatchers.IO) {
        val existingTicket = ticketDao.getCartItemByTicketId(ticket.ticketId)
        if (existingTicket != null) {
            val currentQty = existingTicket.cartQty
            val updatedQty = if (key == "Add") currentQty + 1 else currentQty - 1

            if (updatedQty <= 0) {
                ticketDao.deleteByTicketId(ticket.ticketId)
            } else {
                val newTotalAmount = ticket.ticketAmount * updatedQty
                ticketDao.updateExistingTicket(
                    ticketId = ticket.ticketId,
                    newQty = updatedQty,
                    additionalAmount = newTotalAmount
                )
            }
        } else {
            ticketDao.insertCartItem(ticket)
        }
    }


    suspend fun getTicketsMapByIds(): Map<Int, Ticket> {
        return ticketDao.getAllCart().associateBy { it.ticketId }
    }


    suspend fun getCartItemByTicketId(ticketId: Int): Ticket? = withContext(Dispatchers.IO) {
        ticketDao.getCartItemByTicketId(ticketId)
    }


    suspend fun getCartStatus(): Pair<Double, Boolean> = withContext(Dispatchers.IO) {
        val totalAmount = ticketDao.getCartTotalAmount() ?: 0.0
        val hasData = totalAmount > 0
        Pair(totalAmount, hasData)
    }


    suspend fun getAllTicketsInCart(): List<Ticket> {
        return ticketDao.getAllCart()
    }

    suspend fun updateCartItemsInfo(
        newName: String,
        newPhoneNumber: String,
        newIdno: String,
        newIdProof: String,
        newImg: ByteArray
    ) = withContext(Dispatchers.IO) {
        ticketDao.updateAllCartItems(
            newName = newName,
            newPhoneNumber = newPhoneNumber,
            newIdno = newIdno,
            newIdProof = newIdProof,
            newImg = newImg
        )
    }


    suspend fun deleteTicketById(ticketId: Int) {
        ticketDao.deleteByTicketId(ticketId)
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        ticketDao.truncateTable()
    }
}
