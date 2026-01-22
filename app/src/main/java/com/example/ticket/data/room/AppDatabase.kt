package com.example.ticket.data.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.ticket.data.room.dao.ActiveTicketDao
import com.example.ticket.data.room.dao.CategoryDao
import com.example.ticket.data.room.dao.CompanyDao
import com.example.ticket.data.room.dao.LabelSettingsDao
import com.example.ticket.data.room.dao.TicketDao
import com.example.ticket.data.room.entity.ActiveTicket
import com.example.ticket.data.room.entity.Category
import com.example.ticket.data.room.entity.Company
import com.example.ticket.data.room.entity.LabelSettings
import com.example.ticket.data.room.entity.Ticket


@Database(
    entities = [Company::class, Category::class, ActiveTicket::class, Ticket::class, LabelSettings::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {


    abstract fun companyDao(): CompanyDao
    abstract fun labelSettingsDao(): LabelSettingsDao
    abstract fun categoryDao(): CategoryDao
    abstract fun activeTicketDao(): ActiveTicketDao
    abstract fun ticketDao(): TicketDao



}
