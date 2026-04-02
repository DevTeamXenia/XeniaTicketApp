package com.xenia.ticket.data.network.local

import com.xenia.ticket.data.repository.ActiveTicketRepository
import com.xenia.ticket.data.repository.CategoryRepository
import com.xenia.ticket.data.repository.CompanyRepository
import com.xenia.ticket.data.repository.LabelSettingsRepository
import com.xenia.ticket.utils.common.CompanyKey
import com.xenia.ticket.utils.common.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import retrofit2.HttpException


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
                    ?: return@coroutineScope SyncResult.Error("Token missing", code = 401)

                val companyResult = try {
                    companyRepository.loadCompanySettings(token)
                } catch (e: HttpException) {
                    return@coroutineScope SyncResult.Error(
                        e.message() ?: "Company API failed",
                        code = e.code()
                    )
                } catch (e: Exception) {
                    return@coroutineScope SyncResult.Error(
                        e.message ?: "Company API failed",
                        code = null
                    )
                }

                if (!companyResult)
                    return@coroutineScope SyncResult.Error("Company API failed")

                val categoryEnabled = companyRepository.getBoolean(CompanyKey.CATEGORY_ENABLE)

                val labelApi = async {
                    try {
                        labelSettingsRepository.loadLabelSettings(token)
                    } catch (e: HttpException) {
                        throw SyncException(
                            e.message() ?: "Label API failed",
                            e.code()
                        )
                    } catch (e: Exception) {
                        throw SyncException(
                            e.message ?: "Label API failed"
                        )
                    }
                }

                val offeringApi = async {
                    try {
                        activeTicketRepository.loadTickets(token)
                    } catch (e: HttpException) {
                        throw SyncException(
                            e.message() ?: "Offering API failed",
                            e.code()
                        )
                    } catch (e: Exception) {
                        throw SyncException(
                            e.message ?: "Offering API failed"
                        )
                    }
                }

                val mappingApi = async {
                    try {
                        activeTicketRepository.loadMappings(token)
                    } catch (e: HttpException) {
                        throw SyncException(
                            e.message() ?: "Mapping API failed",
                            e.code()
                        )
                    } catch (e: Exception) {
                        throw SyncException(
                            e.message ?: "Mapping API failed"
                        )
                    }
                }

                val showApi = async {
                    try {
                        activeTicketRepository.loadShows(token)
                    } catch (e: HttpException) {
                        throw SyncException(
                            e.message() ?: "Offering API failed",
                            e.code()
                        )
                    } catch (e: Exception) {
                        throw SyncException(
                            e.message ?: "Offering API failed"
                        )
                    }
                }

                val categoryApi = async {
                    if (categoryEnabled) {
                        try {
                            categoryRepository.loadCategories(token)
                        } catch (e: HttpException) {
                            throw SyncException(
                                e.message() ?: "Category API failed",
                                e.code()
                            )
                        } catch (e: Exception) {
                            throw SyncException(
                                e.message ?: "Category API failed"
                            )
                        }
                    } else true
                }

                try {
                    if (!labelApi.await()) return@coroutineScope SyncResult.Error("Label API failed")
                    if (!offeringApi.await()) return@coroutineScope SyncResult.Error("Offering API failed")
                    if (!mappingApi.await()) return@coroutineScope SyncResult.Error("Mapping API failed")
                    if (!categoryApi.await()) return@coroutineScope SyncResult.Error("Category API failed")
                } catch (e: SyncException) {
                    return@coroutineScope SyncResult.Error(e.message ?: "Unknown sync error", e.code)
                }

                sessionManager.setFirstLoad(false)
                SyncResult.Success("Initial sync completed successfully")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            SyncResult.Error(
                message = e.message ?: "Unknown sync error",
                code = null
            )
        }
    }
}

class SyncException(message: String, val code: Int? = null) : Exception(message)