package com.example.ticket.ui.sreens.billing

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.ticket.R
import com.example.ticket.data.listeners.InactivityHandlerActivity
import com.example.ticket.data.listeners.OnTicketClickListener
import com.example.ticket.data.repository.ActiveTicketRepository
import com.example.ticket.data.repository.TicketRepository
import com.example.ticket.data.room.entity.ActiveTicket
import com.example.ticket.databinding.ActivityBillinTicketBinding
import com.example.ticket.databinding.ActivityTicketBinding
import com.example.ticket.ui.adapter.TicketAdapter
import com.example.ticket.ui.dialog.CustomInactivityDialog
import com.example.ticket.utils.common.CommonMethod.setLocale
import com.example.ticket.utils.common.CommonMethod.showSnackbar
import com.example.ticket.utils.common.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import kotlin.getValue

class Billin_Ticket_Activity : AppCompatActivity(), OnTicketClickListener{
    private lateinit var binding: ActivityBillinTicketBinding
    private val ticketRepository: TicketRepository by inject()
    private val activeTicketRepository: ActiveTicketRepository by inject()
    private lateinit var ticketAdapter: TicketAdapter
    private var categoryId: Int = 0
    private var selectedLanguage: String? = ""
    private val sessionManager: SessionManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBillinTicketBinding.inflate(layoutInflater)

        if (intent.getBooleanExtra("forceLandscape", false)) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        setContentView(binding.root)
        setLocale(this, sessionManager.getSelectedLanguage())
        selectedLanguage = sessionManager.getSelectedLanguage()
        categoryId = intent.getIntExtra("CATEGORY_ID", 0)
        setupUI()
        setupRecyclerViews()
        getTickets()
    }
    private fun setupUI() {
        binding.txtHome?.text = getString(R.string.more_people)
        binding.txtselectTicket.text = getString(R.string.choose_your_tickets)

    }
    @SuppressLint("NotifyDataSetChanged")
    private fun setupRecyclerViews() {
        ticketAdapter =
            TicketAdapter(this, selectedLanguage!!, ticketRepository, lifecycleScope, this)
        binding.ticketRecycler.layoutManager =
            GridLayoutManager(this@Billin_Ticket_Activity, 3)
        binding.ticketRecycler.adapter = ticketAdapter
    }
    private fun getTickets() {
        CoroutineScope(Dispatchers.Main).launch {
            try {

                val token = sessionManager.getToken()

                if (token.isNullOrEmpty()) {
                    binding.ticketRecycler.visibility = View.GONE
                    return@launch
                }
                val isLoaded = activeTicketRepository.loadTickets(token)

                if (!isLoaded) {
                    showSnackbar(binding.root, "No Tickets found.")
                    return@launch
                }

                val tickets = activeTicketRepository.getAllTickets()

                if (tickets.isEmpty()) {
                    showSnackbar(binding.root, "No Tickets found.")
                    return@launch
                }


                val activeTickets = tickets.filter { it.ticketActive }

                if (activeTickets.isNotEmpty()) {

                    ticketAdapter.updateTickets(activeTickets)

                    val updatedMap = ticketRepository.getTicketsMapByIds()
                    ticketAdapter.updateDbItemsMap(updatedMap)

                } else {
                    showSnackbar(binding.root, "No active tickets found.")
                }

            } catch (e: Exception) {
                showSnackbar(binding.root, "Error: ${e.localizedMessage}")
            }
        }
    }

    override fun onTicketClick(darshanItem: ActiveTicket) {

    }

    override fun onTicketClear(darshanItem: ActiveTicket) {

    }

    override fun onTicketAdded() {

    }



}