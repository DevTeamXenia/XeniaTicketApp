package com.xenia.ticket.data.room.dao



import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.xenia.ticket.data.room.entity.CompanyLabels
import kotlinx.coroutines.flow.Flow

@Dao
interface CompanyLabelsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CompanyLabels>)
    @Query("DELETE FROM CompanyLabels")
    suspend fun clearAll()

    @Query("SELECT * FROM CompanyLabels WHERE active = 1")
    suspend fun getActiveLabels(): List<CompanyLabels>

    @Query("SELECT * FROM CompanyLabels WHERE active = 1")
    fun getAllActiveLabels(): Flow<List<CompanyLabels>>

}
