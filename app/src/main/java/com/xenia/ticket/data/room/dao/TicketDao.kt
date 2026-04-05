package com.xenia.ticket.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.xenia.ticket.data.room.entity.Ticket

@Dao
interface TicketDao {
    @Query("SELECT * FROM Ticket WHERE active = 1")
    suspend fun getTickets(): List<Ticket>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tickets: List<Ticket>)

    @Query("DELETE FROM Ticket")
    suspend fun clearAll()

    @Query("""
        SELECT * FROM Ticket 
        WHERE categoryId = :categoryId 
        AND active = 1
""")

    suspend fun getTicketsByCategory(categoryId: Int): List<Ticket>

    @Query("SELECT * FROM Ticket WHERE id IN (:ticketIds)")
    suspend fun getTicketsByIds(ticketIds: List<Int>): List<Ticket>

}
