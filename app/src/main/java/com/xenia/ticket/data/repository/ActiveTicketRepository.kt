package com.xenia.ticket.data.repository

import retrofit2.HttpException
import androidx.room.Transaction
import com.xenia.ticket.data.network.model.TicketComboMappingDto
import com.xenia.ticket.data.network.model.TicketDto
import com.xenia.ticket.data.network.service.ApiClient
import com.xenia.ticket.data.room.dao.ActiveTicketDao
import com.xenia.ticket.data.room.dao.TicketComboMappingDao
import com.xenia.ticket.data.room.entity.ActiveTicket
import com.xenia.ticket.data.room.entity.TicketComboMapping
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ActiveTicketRepository(
    private val ticketDao: ActiveTicketDao
    ,private val mappingDao: TicketComboMappingDao

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


    suspend fun loadMappings(token: String): Boolean {
        return try {
            val response = ApiClient.apiService.getTicketMapping(token)
            val data = response.data ?: emptyList()

            val entities = data.map { it.toEntity() }
            mappingDao.replaceAll(entities)

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getAllMappings(): List<TicketComboMapping> {
        return mappingDao.getAll()
    }

    private fun TicketComboMappingDto.toEntity(): TicketComboMapping {
        return TicketComboMapping(
            id = id,
            parentTicketId = parentTicketId,
            childTicketId = childTicketId,
            createdDate = createdDate
        )
    }
}
