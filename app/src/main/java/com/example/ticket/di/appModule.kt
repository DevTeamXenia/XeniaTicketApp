package com.example.ticket.di

import androidx.room.Room
import com.example.ticket.data.repository.LoginRepository
import com.example.ticket.data.room.AppDatabase
import com.example.ticket.utils.common.SessionManager
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val roomModule = module {

    single {
        Room.databaseBuilder(
            androidApplication(),
            AppDatabase::class.java,
            "XeniaTicketDB"
        )
            .fallbackToDestructiveMigration(true)
            .build()
    }

    single { SessionManager(androidContext()) }
    single { LoginRepository() }
}