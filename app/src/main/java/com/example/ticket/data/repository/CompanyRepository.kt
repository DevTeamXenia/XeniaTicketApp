package com.example.ticket.data.repository

import androidx.room.Transaction
import com.xenia.templekiosk.data.network.model.CompanyResponse
import com.xenia.templekiosk.data.network.service.ApiClient
import com.xenia.templekiosk.data.room.dao.CompanyDao
import com.xenia.templekiosk.data.room.entity.Company
import com.xenia.templekiosk.utils.common.CompanyKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.collections.map
import kotlin.let
import kotlin.text.equals


class CompanyRepository(
    private val companyDao: CompanyDao
) {

    suspend fun loadCompanySettings(bearerToken: String): Boolean {
        return try {
            val apiCompanies = fetchCompanySettings(bearerToken)
            if (apiCompanies.isEmpty()) return false
            val dbCompanies = apiCompanies.map { it.toEntity() }
            refreshCompanies(dbCompanies)

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private suspend fun fetchCompanySettings(
        bearerToken: String
    ): List<CompanyResponse> = withContext(Dispatchers.IO) {
        ApiClient.apiService.getCompanySettings(bearerToken)
    }

    @Transaction
    private suspend fun refreshCompanies(companies: List<Company>) = withContext(Dispatchers.IO) {
        companyDao.deleteCompanies()
        companyDao.insertCompanies(companies)
    }

    suspend fun getCompany(): Company? {
        return companyDao.getCompany()
    }

    suspend fun getBoolean(key: CompanyKey): Boolean {
        return companyDao.getByKeyCode(key.code)
            ?.value
            ?.equals("True", true) == true
    }

    suspend fun getString(key: CompanyKey): String? {
        return companyDao.getByKeyCode(key.code)?.value
    }

    suspend fun getGateway(): String? {
        return getString(CompanyKey.COMPANY_GATEWAY)
    }

    suspend fun getDefaultLanguage(): String? {
        return getString(CompanyKey.DEFAULT_LANGUAGE)
    }
    private fun CompanyResponse.toEntity() = Company(
        companySettingsId = this.companySettingsId,
        companyId = this.companyId,
        keyCode = this.keyCode,
        value = this.value ?: "",
        active = this.active
    )

    suspend fun getCompanyPrintHeaderFile(): File? {
        val path = getString(CompanyKey.COMPANYPRINT_H)
        return path?.let { File(it) }
    }

    suspend fun getCompanyPrintFooterFile(): File? {
        val path = getString(CompanyKey.COMPANYPRINT_F)
        return path?.let { File(it) }
    }


}
