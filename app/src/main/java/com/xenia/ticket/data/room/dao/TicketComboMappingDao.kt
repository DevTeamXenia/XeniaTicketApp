package com.xenia.ticket.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.xenia.ticket.data.room.entity.TicketComboMapping

@Dao
interface TicketComboMappingDao {

    @Query("SELECT * FROM TicketComboMapping")
    suspend fun getAll(): List<TicketComboMapping>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<TicketComboMapping>)

    @Query("DELETE FROM TicketComboMapping")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceAll(items: List<TicketComboMapping>) {
        clearAll()
        insertAll(items)
    }
}