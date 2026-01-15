package com.example.ticket

import android.app.Application
import com.example.ticket.di.roomModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin

class TicketApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@TicketApplication)
            modules(roomModule)
        }
    }
}
