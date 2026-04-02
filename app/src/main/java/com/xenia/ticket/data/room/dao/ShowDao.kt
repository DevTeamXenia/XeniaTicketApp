package com.xenia.ticket.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.xenia.ticket.data.room.entity.Show
@Dao
interface ShowDao {
    @Query("SELECT * FROM Show WHERE isActive = 1 ORDER BY ShowName ASC")
    suspend fun getAllShows(): List<Show>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(show: List<Show>)

    @Query("DELETE FROM Show")
    suspend fun clearAll()

}