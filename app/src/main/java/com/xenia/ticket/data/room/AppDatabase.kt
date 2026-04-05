package com.xenia.ticket.data.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.xenia.ticket.data.room.dao.OrderDao
import com.xenia.ticket.data.room.dao.CategoryDao
import com.xenia.ticket.data.room.dao.CompanySettingsDao
import com.xenia.ticket.data.room.dao.CompanyLabelsDao
import com.xenia.ticket.data.room.dao.ShowDao
import com.xenia.ticket.data.room.dao.TicketComboMappingDao
import com.xenia.ticket.data.room.dao.TicketDao
import com.xenia.ticket.data.room.entity.Category
import com.xenia.ticket.data.room.entity.CompanyLabels
import com.xenia.ticket.data.room.entity.CompanySettings
import com.xenia.ticket.data.room.entity.Show
import com.xenia.ticket.data.room.entity.Orders
import com.xenia.ticket.data.room.entity.Ticket
import com.xenia.ticket.data.room.entity.TicketComboMapping


@Database(
    entities = [
        CompanySettings::class,
        Category::class,
        Orders::class,
        CompanyLabels::class,
        Ticket::class,
        TicketComboMapping::class,
        Show::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun companySettings(): CompanySettingsDao
    abstract fun companyLabelsDao(): CompanyLabelsDao
    abstract fun ticketDao(): TicketDao
    abstract fun orderDao(): OrderDao
    abstract fun ticketComboMappingDao(): TicketComboMappingDao
    abstract fun showDao(): ShowDao

}
