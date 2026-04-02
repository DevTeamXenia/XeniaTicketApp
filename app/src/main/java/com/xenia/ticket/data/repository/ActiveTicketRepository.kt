package com.xenia.ticket.data.repository

import retrofit2.HttpException
import androidx.room.Transaction
import com.xenia.ticket.data.network.model.ActiveItem
import com.xenia.ticket.data.network.model.ShowDto
import com.xenia.ticket.data.network.model.TicketComboMappingDto
import com.xenia.ticket.data.network.model.TicketDto
import com.xenia.ticket.data.network.service.ApiClient
import com.xenia.ticket.data.room.dao.ActiveTicketDao
import com.xenia.ticket.data.room.dao.ShowDao
import com.xenia.ticket.data.room.dao.TicketComboMappingDao
import com.xenia.ticket.data.room.entity.ActiveTicket
import com.xenia.ticket.data.room.entity.Show
import com.xenia.ticket.data.room.entity.TicketComboMapping
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ActiveTicketRepository(
    private val ticketDao: ActiveTicketDao,
    private val mappingDao: TicketComboMappingDao,
    private val showDao: ShowDao

) {

    suspend fun getAllItems(): List<ActiveItem> =
        withContext(Dispatchers.IO) {

            val tickets = ticketDao.getAllActiveTickets().map {
                ActiveItem(
                    id = it.ticketId,
                    name = it.ticketName,
                    nameMa = it.ticketNameMa,
                    nameTa = it.ticketNameTa,
                    nameTe = it.ticketNameTe,
                    nameKa = it.ticketNameKa,
                    nameHi = it.ticketNameHi,
                    namePa = it.ticketNamePa,
                    nameSi = it.ticketNameSi,
                    nameMr = it.ticketNameMr,
                    category = it.ticketCategoryId,
                    companyId = it.ticketCompanyId,
                    amount = it.ticketAmount,
                    active = it.ticketActive,
                    combo = it.ticketCombo,
                    type = "TICKET"
                )
            }

            val shows = showDao.getAllShows().map {
                ActiveItem(
                    id = it.showId,
                    name = it.showName,
                    nameMa = it.showNameMa,
                    nameTa = it.showNameTa,
                    nameTe = it.showNameTe,
                    nameKa = it.showNameKa,
                    nameHi = it.showNameHi,
                    namePa = it.showNamePa,
                    nameSi = it.showNameSi,
                    nameMr = it.showNameMr,
                    category = 0,
                    companyId = it.companyId,
                    amount = 0.00,
                    active = it.isActive,
                    combo = false,
                    type = "SHOW"
                )
            }

            tickets + shows
        }


    suspend fun getItemsByCategory(categoryId: Int): List<ActiveItem> =
        withContext(Dispatchers.IO) {

            val tickets = ticketDao.getTicketsByCategory(categoryId).map {
                ActiveItem(
                    id = it.ticketId,
                    name = it.ticketName,
                    nameMa = it.ticketNameMa,
                    nameTa = it.ticketNameTa,
                    nameTe = it.ticketNameTe,
                    nameKa = it.ticketNameKa,
                    nameHi = it.ticketNameHi,
                    namePa = it.ticketNamePa,
                    nameSi = it.ticketNameSi,
                    nameMr = it.ticketNameMr,
                    category = it.ticketCategoryId,
                    companyId = it.ticketCompanyId,
                    amount = it.ticketAmount,
                    active = it.ticketActive,
                    combo = it.ticketCombo,
                    type = "TICKET"
                )
            }

            val shows = showDao.getAllShows().map {
                ActiveItem(
                    id = it.showId,
                    name = it.showName,
                    nameMa = it.showNameMa,
                    nameTa = it.showNameTa,
                    nameTe = it.showNameTe,
                    nameKa = it.showNameKa,
                    nameHi = it.showNameHi,
                    namePa = it.showNamePa,
                    nameSi = it.showNameSi,
                    nameMr = it.showNameMr,
                    category = 0,
                    companyId = it.companyId,
                    amount = 0.00,
                    active = it.isActive,
                    combo = false,
                    type = "SHOW"
                )
            }

            tickets + shows
        }

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
            ticketActive = ticketActive,
            ticketCombo = ticketCombo
        )
    suspend fun getAllTickets(): List<ActiveTicket> =
        withContext(Dispatchers.IO) {
            ticketDao.getAllActiveTickets()
        }
    suspend fun getTicketsByCategory(categoryId: Int): List<ActiveTicket> =
        withContext(Dispatchers.IO) {
            ticketDao.getTicketsByCategory(categoryId)
        }



    suspend fun loadShows(bearerToken: String): Boolean {
        return try {
            val apiResponse = fetchShows(bearerToken)
            if (apiResponse.isEmpty()) return false

            val entities = apiResponse.map { it.toEntity() }
            refreshShows(entities)
            true
        } catch (e: HttpException) {

            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    private suspend fun fetchShows(
        bearerToken: String
    ): List<ShowDto> = withContext(Dispatchers.IO) {
        ApiClient.apiService
            .getShow(bearerToken)
            .data
    }
    @Transaction
    private suspend fun refreshShows(
        items: List<Show>
    ) = withContext(Dispatchers.IO) {
        showDao.clearAll()
        showDao.insertAll(items)
    }
    private fun ShowDto.toEntity(): Show =
        Show(
            showId = showId,
            showName = showName,
            showNameMa = showNameMa,
            showNameTa = showNameTa,
            showNameTe = showNameTe,
            showNameKa = showNameKa,
            showNameHi = showNameHi,
            showNamePa = showNamePa,
            showNameMr = showNameMr,
            description = description,
            showNameSi = showNameSi,
            durationMinutes = durationMinutes,
            companyId = companyId,
            createdDate = createdDate,
            createdBy = createdBy,
            isActive = isActive
        )



    suspend fun loadMappings(token: String): Boolean {
        return try {
            val data = ApiClient.apiService.getTicketMapping(token)

            val entities = data.map { it.toEntity() }
            mappingDao.replaceAll(entities)

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getComboTickets(parentTicketId: Int): List<ActiveTicket> {
        val mappings = mappingDao.getComboMappings(parentTicketId)

        if (mappings.isEmpty()) return emptyList()

        val childIds = mappings.map { it.childTicketId }

        return ticketDao.getTicketsByIds(childIds)
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
