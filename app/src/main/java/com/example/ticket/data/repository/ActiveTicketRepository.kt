package com.example.ticket.data.repository

import androidx.room.Transaction
import com.example.ticket.data.network.service.ApiClient
import com.example.ticket.data.room.dao.ActiveTicket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ActiveTicketRepository {
}


class TicketRepository(
    private val ticketDao: ActiveTicket
) {

    // Load tickets from API and save in local DB
    suspend fun loadTickets(bearerToken: String): Boolean {
        return try {
            val apiResponse = fetchTickets(bearerToken)
            if (apiResponse.isEmpty()) return false

            val entities = apiResponse.map { it.toEntity() }
            refreshTickets(entities)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Fetch tickets from API
    private suspend fun fetchTickets(bearerToken: String): List<ActiveTicket> = withContext(
        Dispatchers.IO) {
        ApiClient.apiService
            .getTicketList(bearerToken) // Make sure API method returns TicketResponse
            .data
    }

    // Replace all tickets in local DB
    @Transaction
    private suspend fun refreshTickets(items: List<ActiveTicket>) = withContext(Dispatchers.IO) {
        ticketDao.truncateTable()
        ticketDao.insertAll(items)
    }

    // Get all tickets from local DB
    suspend fun getAllTickets(): List<ActiveTicket> = withContext(Dispatchers.IO) {
        ticketDao.getAllCart()
    }

    // Map API DTO to Room entity
    private fun ActiveTicket.toEntity(): ActiveTicket =
        ActiveTicket(
            ticketId = ticketId,
            ticketName = ticketName,
            ticketNameMa = ticketNameMa,
            ticketNameTa = ticketNameTa,
            ticketNameTe = ticketNameTe,
            ticketNameKa = ticketNameKa,
            ticketNameHi = ticketNameHi,
            ticketNamePa = ticketNamePa,
            ticketNameMr = ticketNameMr,
            ticketNameSi = ticketNameSi,
            ticketCategoryId = ticketCategoryId,
            ticketCompanyId = ticketCompanyId,
            ticketAmount = ticketAmount,
            ticketCreatedDate = ticketCreatedDate,
            ticketCreatedBy = ticketCreatedBy,
            ticketActive = ticketActive,
            // Cart fields default to 0 / empty
            cartQty = 0,
            cartTotalAmount = 0.0,
            cartUserName = "",
            cartUserPhone = "",
            cartUserIdNo = "",
            cartUserProof = "",
            cartUserImg = null
        )
}
