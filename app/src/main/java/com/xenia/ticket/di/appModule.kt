package com.xenia.ticket.di

import androidx.room.Room
import com.xenia.ticket.data.network.sync.InitialSyncManager
import com.xenia.ticket.data.repository.TicketRepository
import com.xenia.ticket.data.repository.CategoryRepository
import com.xenia.ticket.data.repository.CompanySettingsRepository
import com.xenia.ticket.data.repository.LabelSettingsRepository
import com.xenia.ticket.data.repository.LoginRepository
import com.xenia.ticket.data.repository.PaymentRepository
import com.xenia.ticket.data.repository.ReportRepository
import com.xenia.ticket.data.repository.OrderRepository
import com.xenia.ticket.data.room.AppDatabase
import com.xenia.ticket.ui.dialog.CustomQRPopupDialogue
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
    single { get<AppDatabase>().companySettings() }
    single { get<AppDatabase>().companyLabelsDao() }
    single { get<AppDatabase>().categoryDao() }
    single { get<AppDatabase>().ticketDao() }
    single { get<AppDatabase>().orderDao() }
    single { get<AppDatabase>().showDao() }
    single { get<AppDatabase>().ticketComboMappingDao() }

    single { SessionManager(androidContext()) }
    single { LoginRepository() }
    single { CompanySettingsRepository(get(),get()) }
    single { LabelSettingsRepository(get()) }
    single { CategoryRepository(get()) }
    single {
        TicketRepository(
            ticketDao = get(),
            mappingDao = get(),
            showDao = get(),
            get()
        )
    }
    single { OrderRepository(get()) }
    single { PaymentRepository() }
    single { ReportRepository(get()) }
    single { ReportPrint(get(), get(), get()) }


    factory { CustomTicketPopupDialogue() }
    factory { CustomQRPopupDialogue() }

    factory {
        InitialSyncManager(
            companyRepository = get(),
            categoryRepository = get(),
            labelSettingsRepository = get(),
            activeTicketRepository = get(),
            sessionManager = get()
        )
    }
}