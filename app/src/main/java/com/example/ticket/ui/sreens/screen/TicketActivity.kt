package com.example.ticket.ui.sreens.screen

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.ticket.R
import com.example.ticket.data.repository.CategoryRepository
import com.example.ticket.data.repository.TicketRepository
import com.example.ticket.databinding.ActivitySelectionBinding
import com.example.ticket.databinding.ActivityTicketBinding
import com.example.ticket.ui.adapter.TicketAdapter
import com.example.ticket.utils.common.CommonMethod.setLocale
import com.example.ticket.utils.common.CommonMethod.showSnackbar
import com.example.ticket.utils.common.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import kotlin.getValue
import kotlin.io.root

class TicketActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTicketBinding
    private val ticketRepository: TicketRepository by inject()
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

        setContentView(binding.root)
        setupRecyclerViews()
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

                val tickets = ticketRepository.getAllTickets()

                if (tickets.isEmpty()) {
                    showSnackbar(binding.root, "No Tickets found.")
                } else {

                    val activeTickets = tickets.filter { it.ticketActive }

                    if (activeTickets.isNotEmpty()) {

                        ticketAdapter.updateTickets(activeTickets)


                        val updatedMap = ticketRepository.getTicketsMapByIds()
                        ticketAdapter.updateDbItemsMap(updatedMap)
                    } else {
                        showSnackbar(binding.root, "No active tickets found.")
                    }
                }
            } catch (e: Exception) {
                showSnackbar(binding.root, "Error: ${e.localizedMessage}")
            }
        }
    }

}