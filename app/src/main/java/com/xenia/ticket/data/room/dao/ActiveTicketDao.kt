package com.xenia.ticket.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.xenia.ticket.data.room.entity.ActiveTicket

@Dao
interface ActiveTicketDao {

    @Query("SELECT * FROM ActiveTickets WHERE ticketActive = 1")
    suspend fun getAllActiveTickets(): List<ActiveTicket>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tickets: List<ActiveTicket>)

    @Query("DELETE FROM ActiveTickets")
    suspend fun clearAll()

    @Query("""
        SELECT * FROM activetickets 
        WHERE ticketCategoryId = :categoryId 
        AND ticketActive = 1
    """)
    suspend fun getTicketsByCategory(categoryId: Int): List<ActiveTicket>

}
