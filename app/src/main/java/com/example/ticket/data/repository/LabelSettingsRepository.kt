package com.example.ticket.data.repository

import androidx.room.Transaction
import com.example.ticket.data.network.model.LabelSettingsResponse
import com.example.ticket.data.network.service.ApiClient
import com.example.ticket.data.room.dao.LabelSettingsDao
import com.example.ticket.data.room.entity.LabelSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlin.collections.map

class LabelSettingsRepository(
    private val labelSettingsDao: LabelSettingsDao
) {

    suspend fun loadLabelSettings(bearerToken: String): Boolean {
        return try {
            val apiResponse = fetchLabelSettings(bearerToken)
            if (apiResponse.isEmpty()) return false

            val entities = apiResponse.map { it.toEntity() }
            refreshLabelSettings(entities)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private suspend fun fetchLabelSettings(
        bearerToken: String
    ): List<LabelSettingsResponse> = withContext(Dispatchers.IO) {
        ApiClient.apiService.getCompanyLabel(bearerToken)
    }

    @Transaction
    private suspend fun refreshLabelSettings(
        items: List<LabelSettings>
    ) = withContext(Dispatchers.IO) {
        labelSettingsDao.clearAll()
        labelSettingsDao.insertAll(items)
    }

    suspend fun getCompanyLabels(
        bearerToken: String
    ): List<LabelSettingsResponse> = withContext(Dispatchers.IO) {
        ApiClient.apiService.getCompanyLabel(bearerToken)
    }
    fun getLabelSettingsFromDb(): Flow<List<LabelSettings>> {
        return labelSettingsDao.getAllActiveLabels()
    }

    private fun LabelSettingsResponse.toEntity(): LabelSettings =
        LabelSettings(
            id = id,
            companyId = companyId,
            settingKey = settingKey,
            displayName = displayName,
            displayNameMa = displayNameMa,
            displayNameTa = displayNameTa,
            displayNameTe = displayNameTe,
            displayNameKa = displayNameKa,
            displayNameHi = displayNameHi,
            displayNameMr = displayNameMr,
            displayNamePa = displayNamePa,
            displayNameSi = displayNameSi,
            createdBy = createdBy,
            createdOn = createdOn,
            modifiedBy = modifiedBy,
            modifiedOn = modifiedOn,
            active = active
        )
}
