package com.xenia.ticket.data.repository

import androidx.room.Transaction
import com.xenia.ticket.data.network.model.CompanyResponse
import com.xenia.ticket.data.network.service.ApiClient
import com.xenia.ticket.data.room.dao.CompanyDao
import com.xenia.ticket.data.room.entity.Company
import com.xenia.ticket.utils.common.CompanyKey

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
    suspend fun getGateway(): String? {
        return getString(CompanyKey.ISPAYMENTGATEWAY)
    }

    suspend fun getBoolean(key: CompanyKey): Boolean {
        return companyDao.getByKeyCode(key.code)
            ?.value
            ?.equals("True", true) == true
    }

    suspend fun getString(key: CompanyKey): String? {
        return companyDao.getByKeyCode(key.code)?.value
    }


    suspend fun getDefaultLanguage(): String? {
        return getString(CompanyKey.DEFAULT_LANGUAGE)
    }
    fun CompanyResponse.toEntity(): Company {
        return Company(
            keyCode = this.keyCode,
            value = this.value,
            applicationId = this.paymentConfig?.applicationId
        )
    }
    suspend fun getCompanyPrintHeaderFile(): File? {
        val path = getString(CompanyKey.COMPANYPRINT_H)
        return path?.let { File(it) }
    }

    suspend fun getCompanyPrintFooterFile(): File? {
        val path = getString(CompanyKey.COMPANYPRINT_F)
        return path?.let { File(it) }
    }


}
