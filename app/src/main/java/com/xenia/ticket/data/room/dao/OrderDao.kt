package com.xenia.ticket.data.room.dao


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.xenia.ticket.data.room.entity.Orders
@Dao
interface OrderDao {

    @Query("SELECT * FROM Orders")
    suspend fun getAllCart(): List<Orders>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartItem(ticket: Orders)

    @Query("SELECT EXISTS(SELECT 1 FROM Orders WHERE ticketId = :ticketId)")
    suspend fun doesTicketExist(ticketId: Int): Boolean

    @Query("""
    UPDATE Orders SET 
        daQty = :newQty,
        daTotalAmount = :additionalAmount,
        screenId = :screenId,
        scheduleId = :scheduleId,
        scheduleDay = :scheduleDay,
        scheduleTime = :scheduleTime,
        screenName = :screenName
    WHERE ticketId = :ticketId
""")
    suspend fun updateExistingTicket(
        ticketId: Int,
        newQty: Int,
        additionalAmount: Double,
        screenId: Int,
        scheduleId: Int,
        scheduleDay: String,
        scheduleTime: String,
        screenName: String
    )

    @Query("SELECT * FROM Orders WHERE ticketId = :ticketId")
    suspend fun getCartItemByTicketId(ticketId: Int): Orders?
    @Query("SELECT COUNT(*) FROM Orders")
    suspend fun getCartCount(): Int

    @Query("SELECT SUM(daTotalAmount) FROM Orders")
    suspend fun getCartTotalAmount(): Double?

    @Query("DELETE FROM Orders WHERE ticketId = :ticketId")
    suspend fun deleteByTicketId(ticketId: Int)

    @Query(
        """UPDATE Orders 
       SET daName = :newName, 
           daPhoneNumber = :newPhoneNumber,
           daProofId = :newIdno,
           daProof = :newIdProof,
           daImg = :newImg
       WHERE 1 = 1"""
    )
    suspend fun updateAllCartItems(
        newName: String,
        newPhoneNumber: String,
        newIdno: String,
        newIdProof: String,
        newImg: ByteArray?
    )

    @Query("DELETE FROM Orders")
    suspend fun truncateTable()
}
