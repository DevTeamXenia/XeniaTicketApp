package com.example.ticket.data.repository

import androidx.room.Query
import com.example.ticket.data.network.model.CategoryItem
import com.example.ticket.data.network.service.ApiClient
import com.example.ticket.data.room.dao.CategoryDao
import com.example.ticket.data.room.entity.Category
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CategoryRepository(
    private val categoryDao: CategoryDao
) {

    suspend fun loadCategories(bearerToken: String): Boolean {
        return try {
            val apiResponse = fetchCategories(bearerToken)
            if (apiResponse.isEmpty()) return false

            val dbEntities = apiResponse.map { it.toEntity() }
            refreshCategories(dbEntities)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private suspend fun fetchCategories(bearerToken: String): List<CategoryItem> =
        withContext(Dispatchers.IO) {
            ApiClient.apiService.getCategory(bearerToken).data
        }

    private suspend fun refreshCategories(categories: List<Category>) = withContext(Dispatchers.IO) {
        categoryDao.deleteAllCategories()
        categoryDao.insertCategories(categories)
    }
    suspend fun getAllCategory(): List<Category> {
        return categoryDao.getAllCategory()
    }
    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        categoryDao.truncateTable()
    }
    private fun CategoryItem.toEntity() = Category(
        categoryId = categoryId,
        categoryName = categoryName,
        categoryNameMa = categoryNameMa,
        categoryNameTa = categoryNameTa,
        categoryNameTe = categoryNameTe,
        categoryNameKa = categoryNameKa,
        categoryNameHi = categoryNameHi,
        categoryNameMr = categoryNameMr,
        categoryNamePa = categoryNamePa,
        categoryNameSi = categoryNameSi,
        CategoryCompanyId = categoryCompanyId,
        categoryCreatedDate = categoryCreatedDate,
        categoryCreatedBy = categoryCreatedBy,
        categoryModifiedDate = categoryModifiedDate,
        categoryModifiedBy = categoryModifiedBy,
        categoryActive = categoryActive
    )

}