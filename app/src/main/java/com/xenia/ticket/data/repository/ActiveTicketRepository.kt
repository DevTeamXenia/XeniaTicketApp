package com.xenia.ticket.data.repository

import retrofit2.HttpException
import androidx.room.Transaction
import com.xenia.ticket.data.network.model.TicketDto
import com.xenia.ticket.data.network.service.ApiClient
import com.xenia.ticket.data.room.dao.ActiveTicketDao
import com.xenia.ticket.data.room.entity.ActiveTicket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ActiveTicketRepository(
    private val ticketDao: ActiveTicketDao

) {

    suspend fun loadTickets(bearerToken: String): Boolean {
        return try {
            val apiResponse = fetchTickets(bearerToken)
            if (apiResponse.isEmpty()) return false

            val entities = apiResponse.map { it.toEntity() }
            refreshTickets(entities)
            true
        } catch (e: HttpException) {

            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    private suspend fun fetchTickets(
        bearerToken: String
    ): List<TicketDto> = withContext(Dispatchers.IO) {
        ApiClient.apiService
            .getTicket(bearerToken)
            .data
    }

    @Transaction
    private suspend fun refreshTickets(
        items: List<ActiveTicket>
    ) = withContext(Dispatchers.IO) {
        ticketDao.clearAll()
        ticketDao.insertAll(items)
    }

    suspend fun getAllTickets(): List<ActiveTicket> =
        withContext(Dispatchers.IO) {
            ticketDao.getAllActiveTickets()
        }
    suspend fun getTicketsByCategory(categoryId: Int): List<ActiveTicket> =
        withContext(Dispatchers.IO) {
            ticketDao.getTicketsByCategory(categoryId)
        }
    private fun TicketDto.toEntity(): ActiveTicket =
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
            ticketActive = ticketActive
        )
}
