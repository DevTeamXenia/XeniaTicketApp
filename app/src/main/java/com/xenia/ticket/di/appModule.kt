package com.xenia.ticket.di

import androidx.room.Room
import com.xenia.ticket.data.network.local.InitialSyncManager
import com.xenia.ticket.data.repository.ActiveTicketRepository
import com.xenia.ticket.data.repository.CategoryRepository
import com.xenia.ticket.data.repository.CompanyRepository
import com.xenia.ticket.data.repository.LabelSettingsRepository
import com.xenia.ticket.data.repository.LoginRepository
import com.xenia.ticket.data.repository.PaymentRepository
import com.xenia.ticket.data.repository.ReportRepository
import com.xenia.ticket.data.repository.TicketRepository
import com.xenia.ticket.data.room.AppDatabase
import com.xenia.ticket.ui.dialog.CustomQRDarshanPopupDialogue
import com.xenia.ticket.ui.dialog.CustomTicketPopupDialogue
import com.xenia.ticket.utils.common.ReportPrint
import com.xenia.ticket.utils.common.SessionManager
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
    single { get<AppDatabase>().companyDao() }
    single { get<AppDatabase>().labelSettingsDao() }
    single { get<AppDatabase>().categoryDao() }
    single { get<AppDatabase>().activeTicketDao() }
    single { get<AppDatabase>().ticketDao() }

    single { SessionManager(androidContext()) }
    single { LoginRepository() }
    single { CompanyRepository(get()) }
    single { LabelSettingsRepository(get()) }
    single { CategoryRepository(get()) }
    single { ActiveTicketRepository(get()) }
    single { TicketRepository(get()) }
    single { PaymentRepository() }
    single { ReportRepository(get()) }
    single { ReportPrint(get(), get(), get()) }



    factory { CustomTicketPopupDialogue() }
    factory { CustomQRDarshanPopupDialogue() }

    factory {
        InitialSyncManager(
            companyRepository = get(),
            categoryRepository = get(),
            labelSettingsRepository = get(),
            activeTicketRepository = get(),
            sessionManager = get(),
            )
    }
}