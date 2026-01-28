package com.xenia.ticket.data.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.xenia.ticket.data.room.dao.ActiveTicketDao
import com.xenia.ticket.data.room.dao.CategoryDao
import com.xenia.ticket.data.room.dao.CompanyDao
import com.xenia.ticket.data.room.dao.LabelSettingsDao
import com.xenia.ticket.data.room.dao.TicketDao
import com.xenia.ticket.data.room.entity.ActiveTicket
import com.xenia.ticket.data.room.entity.Category
import com.xenia.ticket.data.room.entity.Company
import com.xenia.ticket.data.room.entity.LabelSettings
import com.xenia.ticket.data.room.entity.Ticket


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
