package com.example.ticket.data.room.dao

import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Dao
import com.example.ticket.data.room.entity.Company



@Dao
interface CompanyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompanies(companies: List<Company>)

    @Query("SELECT * FROM CompanySettings LIMIT 1")
    suspend fun getCompany(): Company?

    @Query("SELECT * FROM CompanySettings WHERE KeyCode = :keyCode LIMIT 1")
    suspend fun getByKeyCode(keyCode: String): Company?

    @Query("SELECT Value FROM CompanySettings WHERE KeyCode = :key LIMIT 1")
    suspend fun getValueByKey(key: String): String?

    @Query("DELETE FROM CompanySettings")
    suspend fun deleteCompanies()

}
