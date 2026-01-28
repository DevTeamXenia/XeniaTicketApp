package com.example.ticket.data.network.local

import com.example.ticket.data.repository.ActiveTicketRepository
import com.example.ticket.data.repository.CategoryRepository
import com.example.ticket.data.repository.CompanyRepository
import com.example.ticket.data.repository.LabelSettingsRepository
import com.example.ticket.utils.common.CompanyKey
import com.example.ticket.utils.common.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class InitialSyncManager(
    private val companyRepository: CompanyRepository,
    private val categoryRepository: CategoryRepository,
    private val activeTicketRepository: ActiveTicketRepository,
    private val labelSettingsRepository: LabelSettingsRepository,

    private val sessionManager: SessionManager
) {

    suspend fun startInitialLoad(): SyncResult = withContext(Dispatchers.IO) {
        try {
            coroutineScope {

                val token = sessionManager.getToken()
                    ?: return@coroutineScope SyncResult.Error("Token missing")

                if (!companyRepository.loadCompanySettings(token))
                    return@coroutineScope SyncResult.Error("Company API failed")


                val categoryEnabled =
                    companyRepository.getBoolean(CompanyKey.CATEGORY_ENABLE)


                val labelApi = async { labelSettingsRepository.loadLabelSettings(token) }
                val offeringApi = async { activeTicketRepository.loadTickets(token) }



                val categoryApi = async {
                    if (categoryEnabled)
                        categoryRepository.loadCategories(token)
                    else
                        true
                }



                if (!labelApi.await())
                    return@coroutineScope SyncResult.Error("Label API failed")

                if (!offeringApi.await())
                    return@coroutineScope SyncResult.Error("Offering API failed")


                if (!categoryApi.await())
                    return@coroutineScope SyncResult.Error("Category API failed")
                sessionManager.setFirstLoad(false)
                SyncResult.Success("Initial sync completed successfully")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            SyncResult.Error("Initial load failed: ${e.message}")
        }
    }

}
