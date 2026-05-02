package com.xenia.ticket.data.repository

import com.xenia.ticket.data.network.model.LogoutResponse
import com.xenia.ticket.data.network.service.ApiClient.apiService
import com.xenia.ticket.data.room.dao.OrderDao
import com.xenia.ticket.data.room.entity.Orders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class OrderRepository(private val ticketDao: OrderDao) {


    suspend fun insertCartItem(ticket: Orders) = withContext(Dispatchers.IO) {

        val existing = ticketDao.getCartItemByTicketId(ticket.ticketId)

        if (existing != null) {
            ticketDao.updateExistingTicket(
                ticketId = ticket.ticketId,
                newQty = ticket.ticketQty,
                newChildQty = ticket.ticketChildQty,
                additionalAmount = ticket.ticketTotalAmount,
                screenId = ticket.screenId,
                scheduleId = ticket.scheduleId,
                scheduleDay = ticket.scheduleDay,
                scheduleTime = ticket.scheduleTime,
                screenName = ticket.screenName
            )
        } else {
            ticketDao.insertCartItem(ticket)
        }
    }


    suspend fun insertCartBookingItem(ticket: Orders, key: String) = withContext(Dispatchers.IO) {
        val existingTicket = ticketDao.getCartItemByTicketId(ticket.ticketId)
        if (existingTicket != null) {
            val currentQty = existingTicket.ticketQty
            val updatedQty = if (key == "Add") currentQty + 1 else currentQty - 1

            if (updatedQty <= 0) {
                ticketDao.deleteByTicketId(ticket.ticketId)
            } else {
                val newTotalAmount = ticket.ticketTotalAmount * updatedQty
                ticketDao.updateExistingTicket(
                    ticketId = ticket.ticketId,
                    newQty = updatedQty,
                    newChildQty = updatedQty,
                    additionalAmount = newTotalAmount,
                    screenId = ticket.screenId,
                    scheduleId = ticket.scheduleId,
                    scheduleDay = ticket.scheduleDay,
                    scheduleTime = ticket.scheduleTime,
                    screenName = ticket.screenName
                )
            }
        } else {
            ticketDao.insertCartItem(ticket)
        }
    }

    suspend fun getTicketsMapByIds(): Map<Int, Orders> {
        return ticketDao.getAllCart().associateBy { it.ticketId }
    }


    suspend fun getCartItemByTicketId(ticketId: Int): Orders? = withContext(Dispatchers.IO) {
        ticketDao.getCartItemByTicketId(ticketId)
    }


    suspend fun getCartStatus(): Pair<Double, Boolean> = withContext(Dispatchers.IO) {
        val totalAmount = ticketDao.getCartTotalAmount() ?: 0.0
        val hasData = ticketDao.getCartCount() > 0
        Pair(totalAmount, hasData)
    }


    suspend fun getAllTicketsInCart(): List<Orders> {
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

    suspend fun logout(token: String): LogoutResponse {
        return apiService.logout(token)
    }


}