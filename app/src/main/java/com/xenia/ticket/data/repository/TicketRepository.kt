package com.xenia.ticket.data.repository

import retrofit2.HttpException
import androidx.room.Transaction
import com.xenia.ticket.data.network.model.ActiveItem
import com.xenia.ticket.data.network.model.ShowDto
import com.xenia.ticket.data.network.model.ShowScheduleResponse
import com.xenia.ticket.data.network.model.TicketComboMappingDto
import com.xenia.ticket.data.network.model.Tickets
import com.xenia.ticket.data.network.service.ApiClient
import com.xenia.ticket.data.room.dao.ShowDao
import com.xenia.ticket.data.room.dao.TicketComboMappingDao
import com.xenia.ticket.data.room.dao.TicketDao
import com.xenia.ticket.data.room.entity.Show
import com.xenia.ticket.data.room.entity.Ticket
import com.xenia.ticket.data.room.entity.TicketComboMapping
import com.xenia.ticket.utils.common.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TicketRepository(
    private val ticketDao: TicketDao,
    private val mappingDao: TicketComboMappingDao,
    private val showDao: ShowDao,
    private val sessionManager: SessionManager
) {
    suspend fun getAllItems(): List<ActiveItem> =
        withContext(Dispatchers.IO) {

            val tickets = ticketDao.getTickets().map {
                ActiveItem(
                    id = it.id,
                    name = it.name,
                    nameMa = it.nameMa,
                    nameTa = it.nameTa,
                    nameTe = it.nameTe,
                    nameKa = it.nameKa,
                    nameHi = it.nameHi,
                    namePa = it.namePa,
                    nameSi = it.nameSi,
                    nameMr = it.nameMr,
                    category = it.categoryId,
                    companyId = it.companyId,
                    amount = it.amount,
                    active = it.active,
                    combo = it.combo,
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
                    amount = it.amount,
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
                    id = it.id,
                    name = it.name,
                    nameMa = it.nameMa,
                    nameTa = it.nameTa,
                    nameTe = it.nameTe,
                    nameKa = it.nameKa,
                    nameHi = it.nameHi,
                    namePa = it.namePa,
                    nameSi = it.nameSi,
                    nameMr = it.nameMr,
                    category = it.categoryId,
                    companyId = it.companyId,
                    amount = it.amount,
                    active = it.active,
                    combo = it.combo,
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
    ): List<Tickets> = withContext(Dispatchers.IO) {
        ApiClient.apiService
            .getTicket(bearerToken)
            .data
    }

    @Transaction
    private suspend fun refreshTickets(
        items: List<Ticket>
    ) = withContext(Dispatchers.IO) {
        ticketDao.clearAll()
        ticketDao.insertAll(items)
    }

    private fun Tickets.toEntity(): Ticket =
        Ticket(
            id = ticketId,
            name = ticketName,
            nameMa = ticketNameMa,
            nameTa = ticketNameTa,
            nameTe = ticketNameTe,
            nameKa = ticketNameKa,
            nameHi = ticketNameHi,
            namePa = ticketNamePa,
            nameMr = ticketNameMr,
            nameSi = ticketNameSi,
            categoryId = ticketCategoryId,
            companyId = ticketCompanyId,
            amount = ticketAmount,
            createdDate = ticketCreatedDate,
            createdBy = ticketCreatedBy,
            active = ticketActive,
            combo = ticketCombo
        )

    suspend fun getAllTickets(): List<Ticket> =
        withContext(Dispatchers.IO) {
            ticketDao.getTickets()
        }

    suspend fun getTicketsByCategory(categoryId: Int): List<Ticket> =
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
            amount = amount,
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

    suspend fun getComboTickets(parentTicketId: Int): List<Ticket> {
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

    suspend fun getSchedules(id: Int, day: String): List<ShowScheduleResponse> {
        return ApiClient.apiService.getSchedules(sessionManager.getToken().toString(), id, day)
    }

}
