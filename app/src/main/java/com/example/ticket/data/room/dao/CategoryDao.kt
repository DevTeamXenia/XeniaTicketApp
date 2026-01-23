package com.example.ticket.data.room.dao

import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Dao
import com.example.ticket.data.room.entity.Category


@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<Category>)

    @Query("DELETE FROM category")
    suspend fun deleteAllCategories()

    @Query("SELECT * FROM category WHERE categoryActive = 1")
    suspend fun getAllCategory(): List<Category>
    @Query("DELETE FROM category")
    suspend fun truncateTable()
    }

