package com.example.ticket.ui.sreens.billing

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.ticket.R
import com.example.ticket.data.listeners.InactivityHandlerActivity
import com.example.ticket.data.listeners.OnTicketClickListener
import com.example.ticket.data.network.model.TicketDto
import com.example.ticket.data.repository.ActiveTicketRepository
import com.example.ticket.data.repository.TicketRepository
import com.example.ticket.data.room.entity.ActiveTicket
import com.example.ticket.data.room.entity.Ticket
import com.example.ticket.databinding.ActivityBillinTicketBinding
import com.example.ticket.databinding.ActivityTicketBinding
import com.example.ticket.ui.adapter.TicketAdapter
import com.example.ticket.ui.dialog.CustomInactivityDialog
import com.example.ticket.ui.dialog.CustomTicketPopupDialogue
import com.example.ticket.ui.sreens.screen.IdProofActivity
import com.example.ticket.utils.common.CommonMethod.setLocale
import com.example.ticket.utils.common.CommonMethod.showSnackbar
import com.example.ticket.utils.common.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private var formattedTotalAmount: String = ""

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
        lifecycleScope.launch {
            updateCartUI()
        }

        binding.btnProceed.setOnClickListener {
            val intent = Intent(applicationContext, IdProofActivity::class.java)
            intent.putExtra("ITEM_TOTAL", formattedTotalAmount)
            startActivity(intent)
        }
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
        lifecycleScope.launch {
            try {
                val token = sessionManager.getToken()
                if (token.isNullOrEmpty()) return@launch

                val isLoaded = activeTicketRepository.loadTickets(token)
                if (!isLoaded) {
                    showSnackbar(binding.root, "No Tickets found")
                    return@launch
                }

                val tickets = activeTicketRepository.getTicketsByCategory(categoryId)

                if (tickets.isEmpty()) {
                    showSnackbar(binding.root, "No tickets for this category")
                    return@launch
                }

                // Entity → DTO
                val ticketDtos = tickets.map { it.toDto() }

                ticketAdapter.updateTickets(ticketDtos)

            } catch (e: Exception) {
                showSnackbar(binding.root, "Error: ${e.message}")
            }
        }
    }

    override fun onTicketClick(ticketItem: TicketDto) {

        val dialog = CustomTicketPopupDialogue()

        dialog.setData(
            ticketId = ticketItem.ticketId,
            ticketName = ticketItem.ticketName,
            ticketNameMa = ticketItem.ticketNameMa ?: "",
            ticketNameTa = ticketItem.ticketNameTa ?: "",
            ticketNameKa = ticketItem.ticketNameKa ?: "",
            ticketNameTe = ticketItem.ticketNameTe ?: "",
            ticketNameHi = ticketItem.ticketNameHi ?: "",
            ticketNameSi = ticketItem.ticketNameSi ?: "",
            ticketNamePa = ticketItem.ticketNamePa ?: "",
            ticketNameMr = ticketItem.ticketNameMr ?: "",
            ticketCtegoryId = ticketItem.ticketCategoryId,
            ticketCompanyId = ticketItem.ticketCompanyId,
            ticketRate = ticketItem.ticketAmount
        )

        dialog.setListener(this)

        dialog.show(supportFragmentManager, "CustomPopup")
    }


    override fun onTicketClear(darshanItem: TicketDto) {
        lifecycleScope.launch {
            ticketRepository.deleteTicketById(darshanItem.ticketId)
            getTickets()
            updateCartUI()
        }
    }

    override fun onTicketAdded() {
        lifecycleScope.launch {
            getTickets()
            updateCartUI()
        }
    }

    override fun onRestart() {
        super.onRestart()
        setupRecyclerViews()
        getTickets()
        lifecycleScope.launch {
            updateCartUI()
        }
    }
    @SuppressLint("SetTextI18n", "DefaultLocale")
    private suspend fun updateCartUI() {
        val (totalAmount, hasData) = ticketRepository.getCartStatus()

        withContext(Dispatchers.Main) {
            if (hasData) {
                formattedTotalAmount = String.format("%.2f", totalAmount)
                binding.btnProceed.text =
                    getString(R.string.proceed) + "  Rs.$formattedTotalAmount"

                binding.btnProceed.isEnabled = true
                binding.btnProceed.setBackgroundColor(
                    ContextCompat.getColor(
                        this@Billin_Ticket_Activity,
                        R.color.primaryColor
                    )
                )
            } else {
                binding.btnProceed.text = getString(R.string.proceed)
                binding.btnProceed.isEnabled = false
                binding.btnProceed.setBackgroundColor(
                    ContextCompat.getColor(
                        this@Billin_Ticket_Activity,
                        R.color.light_grey
                    )
                )
            }
        }
    }


    fun ActiveTicket.toDto() = TicketDto(
        ticketId = ticketId,
        ticketName = ticketName,
        ticketNameMa = ticketNameMa,
        ticketNameTa = ticketNameTa,
        ticketNameTe = ticketNameTe,
        ticketNameKa = ticketNameKa,
        ticketNameHi = ticketNameHi,
        ticketNamePa = ticketNamePa,
        ticketNameMr = ticketNameMr,
        ticketNameSi = ticketNameSi,
        ticketCategoryId = ticketCategoryId,
        ticketCompanyId = ticketCompanyId,
        ticketAmount = ticketAmount,
        ticketCreatedDate = ticketCreatedDate,
        ticketCreatedBy = ticketCreatedBy,
        ticketActive = ticketActive
    )

}