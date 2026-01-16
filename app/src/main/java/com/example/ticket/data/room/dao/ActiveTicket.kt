package com.example.ticket.data.room.dao

import androidx.room.OnConflictStrategy
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
@Dao

interface ActiveTicket {

    @Query("SELECT * FROM tickets")
    suspend fun getAllTickets(): List<ActiveTicket>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tickets: List<ActiveTicket>)

    @Query("DELETE FROM tickets")
    suspend fun clearAll()
}
