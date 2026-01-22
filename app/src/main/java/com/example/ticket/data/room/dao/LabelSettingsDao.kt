package com.example.ticket.data.room.dao



import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.ticket.data.room.entity.LabelSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface LabelSettingsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<LabelSettings>)

    @Query("DELETE FROM CompanyLabel")
    suspend fun clearAll()

    @Query("SELECT * FROM CompanyLabel WHERE active = 1")
    suspend fun getActiveLabels(): List<LabelSettings>

    @Query("SELECT * FROM CompanyLabel WHERE active = 1")
    fun getAllActiveLabels(): Flow<List<LabelSettings>>

}
