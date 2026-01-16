package com.example.ticket.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.ticket.data.room.entity.ActiveTicket

@Dao
interface ActiveTicketDao {

    @Query("SELECT * FROM ActiveTickets")
    suspend fun getAllTickets(): List<ActiveTicket>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tickets: List<ActiveTicket>)

    @Query("DELETE FROM ActiveTickets")
    suspend fun clearAll()
}
