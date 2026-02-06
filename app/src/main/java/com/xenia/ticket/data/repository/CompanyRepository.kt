package com.xenia.ticket.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.room.Transaction
import com.xenia.ticket.data.network.model.CompanyResponse
import com.xenia.ticket.data.network.service.ApiClient
import com.xenia.ticket.data.room.dao.CompanyDao
import com.xenia.ticket.data.room.entity.Company
import com.xenia.ticket.utils.common.CompanyKey

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import kotlin.collections.map
import kotlin.let
import kotlin.text.equals


class CompanyRepository(
    private val companyDao: CompanyDao,
    context: Context

) {
    private val cacheDirectory: File = context.cacheDir
    suspend fun loadCompanySettings(bearerToken: String): Boolean {
        return try {
            val apiCompanies = fetchCompanySettings(bearerToken)
            if (apiCompanies.isEmpty()) return false
            cacheCompanyImages(apiCompanies, cacheDirectory)
            val dbCompanies = apiCompanies.map { it.toEntity() }
            refreshCompanies(dbCompanies)

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    suspend fun cacheCompanyImages(apiCompanies: List<CompanyResponse>, cacheDir: File) {
        if (apiCompanies.isEmpty()) return

        // Map keyCode -> value
        val companyMap = apiCompanies.associate { it.keyCode to it.value }

        // Get URLs by key
        val headerUrl = companyMap["COMPANYPRINT_H"]
        val footerUrl = companyMap["COMPANYPRINT_F"]
        val logoUrl = companyMap["COMPANYLOGO_P"]

        headerUrl?.let { downloadAndCacheImage(it, "company_header.png", cacheDir) }
        footerUrl?.let { downloadAndCacheImage(it, "company_footer.png", cacheDir) }
        logoUrl?.let { downloadAndCacheImage(it, "company_logo.png", cacheDir) }
    }
    suspend fun downloadAndCacheImage(imageUrl: String, fileName: String, cacheDir: File): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(cacheDir, fileName)
                if (file.exists()) {
                    BitmapFactory.decodeFile(file.absolutePath)
                } else {
                    val bitmap = BitmapFactory.decodeStream(URL(imageUrl).openStream())
                    bitmap?.let {
                        FileOutputStream(file).use { out ->
                            it.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                    }
                    bitmap
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    fun loadCachedBitmap(fileName: String, cacheDir: File): Bitmap? {
        val file = File(cacheDir, fileName)
        return if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
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
