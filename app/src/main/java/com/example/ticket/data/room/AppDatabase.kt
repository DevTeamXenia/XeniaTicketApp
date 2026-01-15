package com.example.ticket.data.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.ticket.data.room.dao.CompanyDao
import com.example.ticket.data.room.entity.Company


@Database(
    entities = [Company::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {


    abstract fun companyDao(): CompanyDao



}
