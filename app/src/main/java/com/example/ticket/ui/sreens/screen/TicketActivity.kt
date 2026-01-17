package com.example.ticket.ui.sreens.screen

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.ticket.R
import com.example.ticket.data.listeners.InactivityHandlerActivity
import com.example.ticket.data.listeners.OnTicketClickListener
import com.example.ticket.data.repository.ActiveTicketRepository
import com.example.ticket.data.repository.TicketRepository
import com.example.ticket.data.room.entity.ActiveTicket
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

class TicketActivity : AppCompatActivity(), OnTicketClickListener,
CustomInactivityDialog.InactivityCallback,
    InactivityHandlerActivity {

    private lateinit var binding: ActivityTicketBinding
    private val ticketRepository: TicketRepository by inject()
    private val activeTicketRepository: ActiveTicketRepository by inject()
    private lateinit var ticketAdapter: TicketAdapter
    private var categoryId: Int = 0
    private var selectedLanguage: String? = ""
    private val sessionManager: SessionManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTicketBinding.inflate(layoutInflater)
        setLocale(this, sessionManager.getSelectedLanguage())
        selectedLanguage = sessionManager.getSelectedLanguage()
        categoryId = intent.getIntExtra("CATEGORY_ID", 0)

        setupUI()
        setContentView(binding.root)
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
            GridLayoutManager(this@TicketActivity, 3)
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

    override fun resetInactivityTimer() {
       
    }

}