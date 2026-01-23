package com.example.ticket.data.room.dao



import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.ticket.data.room.entity.Ticket


@Dao
interface TicketDao {

    @Query("SELECT * FROM tickets")
    suspend fun getAllCart(): List<Ticket>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartItem(ticket: Ticket)

    @Query("SELECT EXISTS(SELECT 1 FROM tickets WHERE ticketId = :ticketId)")
    suspend fun doesTicketExist(ticketId: Int): Boolean

    @Query("UPDATE tickets SET daQty = :newQty, daTotalAmount = :additionalAmount WHERE ticketId = :ticketId")
    suspend fun updateExistingTicket(ticketId: Int, newQty: Int, additionalAmount: Double)

    @Query("SELECT * FROM tickets WHERE ticketId = :ticketId")
    suspend fun getCartItemByTicketId(ticketId: Int): Ticket?
    @Query("SELECT COUNT(*) FROM tickets")
    suspend fun getCartCount(): Int

    @Query("SELECT SUM(daTotalAmount) FROM tickets")
    suspend fun getCartTotalAmount(): Double?

    @Query("DELETE FROM tickets WHERE ticketId = :ticketId")
    suspend fun deleteByTicketId(ticketId: Int)

    @Query(
        """UPDATE tickets 
           SET daName = :newName, 
               daPhoneNumber = :newPhoneNumber,
               daProofId = :newIdno,
               daProof = :newIdProof,
               daImg = :newImg"""
    )
    suspend fun updateAllCartItems(
        newName: String,
        newPhoneNumber: String,
        newIdno: String,
        newIdProof: String,
        newImg: ByteArray
    )

    @Query("DELETE FROM tickets")
    suspend fun truncateTable()
}
