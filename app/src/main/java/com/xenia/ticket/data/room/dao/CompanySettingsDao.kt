package com.xenia.ticket.data.room.dao

import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Dao
import com.xenia.ticket.data.room.entity.CompanySettings


@Dao
interface CompanySettingsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompanies(companies: List<CompanySettings>)

    @Query("SELECT * FROM CompanySettings LIMIT 1")
    suspend fun getCompany(): CompanySettings?

    @Query("SELECT * FROM CompanySettings WHERE KeyCode = :keyCode LIMIT 1")
    suspend fun getByKeyCode(keyCode: String): CompanySettings?

    @Query("SELECT Value FROM CompanySettings WHERE KeyCode = :key LIMIT 1")
    suspend fun getValueByKey(key: String): String?

    @Query("DELETE FROM CompanySettings")
    suspend fun deleteCompanies()

}
