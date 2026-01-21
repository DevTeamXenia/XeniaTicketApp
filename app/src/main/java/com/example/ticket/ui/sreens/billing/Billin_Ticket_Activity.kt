package com.example.ticket.ui.sreens.billing

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ticket.R
import com.example.ticket.data.listeners.OnBookingTicketClick
import com.example.ticket.data.listeners.OnTicketClickListener
import com.example.ticket.data.network.model.TicketDto
import com.example.ticket.data.repository.ActiveTicketRepository
import com.example.ticket.data.repository.CategoryRepository
import com.example.ticket.data.repository.CompanyRepository
import com.example.ticket.data.repository.TicketRepository
import com.example.ticket.data.room.entity.ActiveTicket
import com.example.ticket.data.room.entity.Category
import com.example.ticket.data.room.entity.Ticket
import com.example.ticket.databinding.ActivityBillinTicketBinding
import com.example.ticket.ui.adapter.CategoryAdapter
import com.example.ticket.ui.adapter.TicketBookingAdapter
import com.example.ticket.ui.adapter.TicketCartAdapter
import com.example.ticket.ui.dialog.CustomTicketPopupDialogue
import com.example.ticket.ui.sreens.screen.LanguageActivity
import com.example.ticket.utils.common.CommonMethod.getScreenSize
import com.example.ticket.utils.common.CommonMethod.isLandscapeScreen
import com.example.ticket.utils.common.CommonMethod.setLocale
import com.example.ticket.utils.common.CommonMethod.showSnackbar
import com.example.ticket.utils.common.CompanyKey
import com.example.ticket.utils.common.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import kotlin.getValue

class Billin_Ticket_Activity : AppCompatActivity(), OnTicketClickListener,
    CategoryAdapter.OnCategoryClickListener, OnBookingTicketClick,
    TicketCartAdapter.OnTicketCartClickListener {
    private lateinit var binding: ActivityBillinTicketBinding
    private val ticketRepository: TicketRepository by inject()
    private val activeTicketRepository: ActiveTicketRepository by inject()
    private val categoryRepository: CategoryRepository by inject()
    private lateinit var ticketCartAdapter: TicketCartAdapter
    private val sessionManager: SessionManager by inject()
    private val companyRepository: CompanyRepository by inject()
    private lateinit var categoryAdapter: CategoryAdapter

    private val customTicketPopupDialogue: CustomTicketPopupDialogue by inject()
    private lateinit var ticketAdapter: TicketBookingAdapter
    private var selectedProofMode: String = ""
    private var selectedLanguage: String? = ""
    private var selectedCategoryId: Int = 0
    private var backPressedTime: Long = 0
    private var toast: Toast? = null

    private lateinit var ticketItemsItems: TicketDto
    private var formattedTotalAmount: String = ""
    val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            val currentTime = System.currentTimeMillis()
            if (currentTime - backPressedTime < 2000) {
                toast?.cancel()
                finishAffinity()
            } else {
                backPressedTime = currentTime
                toast = Toast.makeText(
                    this@Billin_Ticket_Activity,
                    "Press back again to exit",
                    Toast.LENGTH_SHORT
                )
                toast?.show()
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBillinTicketBinding.inflate(layoutInflater)

        if (intent.getBooleanExtra("forceLandscape", false)) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        setContentView(binding.root)
        onBackPressedDispatcher.addCallback(this, callback)

        selectedLanguage = sessionManager.getBillingSelectedLanguage()
        setLocale(this, selectedLanguage)
        setupUI()
        setContentView(binding.root)
        setupRecyclerViews()
        lifecycleScope.launch {
            updateCartUI()
        }
        getCategory()

    }
    private fun setupUI() {
        binding.txtHome?.text = getString(R.string.home)
        binding.txtselectTicket.text = getString(R.string.choose_your_tickets)
        binding.btnProceed.text = getString(R.string.proceed)
        binding.btnProceed.setOnClickListener {
            val intent = Intent(applicationContext, Billing_Cart_Activity::class.java)

            startActivity(intent)
        }
        binding.linHome?.setOnClickListener {
            startActivity(Intent(applicationContext, LanguageActivity::class.java))
            finish()

        }
    }
    @SuppressLint("NotifyDataSetChanged")
    private fun setupRecyclerViews() {
        categoryAdapter = CategoryAdapter(this, selectedLanguage!!, this)

        binding.ticketCat.layoutManager = LinearLayoutManager(this)
        binding.ticketCat.adapter = categoryAdapter

        ticketAdapter =
            TicketBookingAdapter(selectedLanguage!!, this, this)
        val spanCount = if (isLandscapeScreen(applicationContext)) {
            8
        } else {
            val screenSize = getScreenSize(applicationContext)
            if (screenSize == "Small") 2 else 2
        }
        binding.ticketRecycler.layoutManager =
            GridLayoutManager(this@Billin_Ticket_Activity, spanCount)
        binding.ticketRecycler.adapter = ticketAdapter


        binding.relCart.layoutManager = LinearLayoutManager(this)
        ticketCartAdapter = TicketCartAdapter(this, selectedLanguage!!, "Booking", this)
        binding.relCart.adapter = ticketCartAdapter
        getCategory()
    }

    private fun getCategory() {
        lifecycleScope.launch {
            try {
                val token = sessionManager.getToken()

                if (token.isNullOrEmpty()) {
                    binding.ticketCat.visibility = View.GONE
                    return@launch
                }

                val isLoaded = withContext(Dispatchers.IO) {
                    categoryRepository.loadCategories(token)
                }

                if (!isLoaded) {
                    binding.ticketCat.visibility = View.GONE
                    return@launch
                }

                val categoryEntities = withContext(Dispatchers.IO) {
                    categoryRepository.getAllCategory()
                }

                if (categoryEntities.isEmpty()) {
                    binding.ticketCat.visibility = View.GONE
                    return@launch
                }

                val activeCategories = categoryEntities.filter { it.categoryActive }

                if (activeCategories.isEmpty()) {
                    binding.ticketCat.visibility = View.GONE
                    return@launch
                }

                binding.ticketCat.visibility =
                    if (companyRepository.getBoolean(CompanyKey.CATEGORY_ENABLE))
                        View.VISIBLE
                    else View.GONE

                val selectedCategory = activeCategories.first()
                selectedCategoryId = selectedCategory.categoryId

                val categories = activeCategories.map { entity ->
                    Category(
                        categoryId = entity.categoryId,
                        categoryName = entity.categoryName,
                        categoryNameMa = entity.categoryNameMa,
                        categoryNameTa = entity.categoryNameTa,
                        categoryNameTe = entity.categoryNameTe,
                        categoryNameKa = entity.categoryNameKa,
                        categoryNameHi = entity.categoryNameHi,
                        categoryNameMr = entity.categoryNameMr,
                        categoryNamePa = entity.categoryNamePa,
                        categoryNameSi = entity.categoryNameSi,
                        CategoryCompanyId = entity.CategoryCompanyId,
                        categoryCreatedDate = entity.categoryCreatedDate,
                        categoryCreatedBy = entity.categoryCreatedBy,
                        categoryModifiedDate = entity.categoryModifiedDate,
                        categoryModifiedBy = entity.categoryModifiedBy,
                        categoryActive = entity.categoryActive
                    )
                }
                categoryAdapter.updateCategories(categories)
                getTickets(selectedCategoryId)
            } catch (e: Exception) {
                Log.e("DEBUG", "Category load error", e)
                binding.ticketCat.visibility = View.GONE
            }
        }
    }

    private fun getTickets(categoryId: Int) {
        lifecycleScope.launch {
            try {
                val token = sessionManager.getToken()
                if (token.isNullOrEmpty()) return@launch

                val isLoaded = activeTicketRepository.loadTickets(token)
                if (!isLoaded) {
                    showSnackbar(binding.root, "No Tickets found")
                    return@launch
                }

                val tickets =
                    activeTicketRepository.getTicketsByCategory(categoryId)

                val ticketDtos = tickets.map { it.toDto() }
                ticketAdapter.updateTickets(ticketDtos)
                val updatedMap = ticketRepository.getTicketsMapByIds()
                ticketAdapter.updateDbItemsMap(updatedMap)

                if (tickets.isEmpty()) {
                    showSnackbar(binding.root, "No tickets for this category")
                }
                getCartTicket()
            } catch (e: Exception) {
                showSnackbar(binding.root, "Error: ${e.message}")
            }
        }
    }


    override fun onTicketClick(ticketItem: TicketDto) {
        runOnUiThread {

            ticketItemsItems = ticketItem

            customTicketPopupDialogue.setData(
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
            customTicketPopupDialogue.setListener(this)
            if (!customTicketPopupDialogue.isAdded) {
                customTicketPopupDialogue.show(
                    supportFragmentManager,
                    "CustomPopup"
                )
            }
        }
    }


    override fun onTicketClear(ticketItem: TicketDto) {
        lifecycleScope.launch {
            ticketItemsItems = ticketItem
            ticketRepository.deleteTicketById(ticketItem.ticketId)
            getTickets(selectedCategoryId)
            updateCartUI()
        }
    }

    override fun onTicketAdded() {
        lifecycleScope.launch {
            getTickets(selectedCategoryId)
            updateCartUI()
        }
    }

    override fun onRestart() {
        super.onRestart()
        setupRecyclerViews()
        getCategory()
        getTickets(selectedCategoryId)
        lifecycleScope.launch {
            updateCartUI()
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

    override fun onCategoryClick(category: Category) {
        selectedCategoryId = category.categoryId
        getTickets(selectedCategoryId)
    }

    override fun onTicketMinusClick(ticketItem: TicketDto) {
        lifecycleScope.launch {

            val existingItem =
                ticketRepository.getCartItemByTicketId(ticketItem.ticketId)

            val currentQty = existingItem?.daQty ?: 0

            if (currentQty > 0) {

                val newQty = currentQty - 1

                if (newQty == 0) {


                    ticketRepository.deleteTicketById(ticketItem.ticketId)

                } else {

                    val totalAmount = ticketItem.ticketAmount * newQty

                    val cartItem = Ticket(
                        id = existingItem?.id ?: 0L,
                        ticketId = ticketItem.ticketId,
                        ticketName = ticketItem.ticketName,
                        ticketNameMa = ticketItem.ticketNameMa,
                        ticketNameTa = ticketItem.ticketNameTa,
                        ticketNameTe = ticketItem.ticketNameTe,
                        ticketNameKa = ticketItem.ticketNameKa,
                        ticketNameHi = ticketItem.ticketNameHi,
                        ticketNamePa = ticketItem.ticketNamePa,
                        ticketNameMr = ticketItem.ticketNameMr,
                        ticketNameSi = ticketItem.ticketNameSi,
                        ticketCategoryId = ticketItem.ticketCategoryId,
                        ticketCompanyId = ticketItem.ticketCompanyId,
                        ticketAmount = ticketItem.ticketAmount,
                        ticketTotalAmount = totalAmount,
                        ticketCreatedDate = existingItem?.ticketCreatedDate ?: "",
                        ticketCreatedBy = existingItem?.ticketCreatedBy ?: 1,
                        ticketActive = true,
                        daName = binding.editName?.text.toString(),
                        daRate = ticketItem.ticketAmount,
                        daQty = newQty,
                        daTotalAmount = totalAmount,
                        daPhoneNumber = binding.editPhNo?.text.toString(),
                        daCustRefNo = "",
                        daNpciTransId = "",
                        daProofId = binding.editId?.text.toString(),
                        daProof = selectedProofMode,
                        daImg = byteArrayOf()
                    )

                    ticketRepository.insertCartBookingItem(cartItem, "Sub")
                }

                val updatedMap = ticketRepository.getTicketsMapByIds()
                ticketAdapter.updateDbItemsMap(updatedMap)
                getCartTicket()
            }
        }
    }

    @SuppressLint("SetTextI18n", "DefaultLocale", "SuspiciousIndentation")
    private fun getCartTicket() {
        lifecycleScope.launch {
            val allDarshanTickets = ticketRepository.getAllTicketsInCart()
            ticketCartAdapter.updateTickets(allDarshanTickets)
            lifecycleScope.launch {
                updateCartUI()
            }
        }
    }

    @SuppressLint("DefaultLocale", "SetTextI18n")
    private fun updateCartUI() {
        lifecycleScope.launch {
            val (totalAmount) = ticketRepository.getCartStatus()
            formattedTotalAmount = String.format("%.2f", totalAmount)
            val hasUserAmount = !binding.editName.text.isNullOrBlank()

            if (totalAmount > 0 && hasUserAmount) {
                binding.btnProceed.text = getString(R.string.pay) + " Rs. " + formattedTotalAmount
                binding.btnProceed.setBackgroundColor(
                    ContextCompat.getColor(this@Billin_Ticket_Activity, R.color.primaryColor)
                )
                binding.btnProceed.isEnabled = true
            } else if (totalAmount > 0) {
                if (!isLandscapeScreen(applicationContext)) {
                    binding.btnProceed.text =
                        getString(R.string.pay) + " Rs. " + formattedTotalAmount
                    binding.btnProceed.setBackgroundColor(
                        ContextCompat.getColor(this@Billin_Ticket_Activity, R.color.primaryColor)
                    )
                    binding.btnProceed.isEnabled = true
                }

            } else {
                binding.btnProceed.text = getString(R.string.pay) + " Rs. " + formattedTotalAmount
                binding.btnProceed.setBackgroundColor(
                    ContextCompat.getColor(this@Billin_Ticket_Activity, R.color.light_grey)
                )
                binding.btnProceed.isEnabled = false
            }
        }
    }

    override fun onTicketPlusClick(ticketItem: TicketDto) {
        lifecycleScope.launch {
            val cartItem = Ticket(
                ticketId = ticketItem.ticketId,
                ticketName = ticketItem.ticketName,
                ticketNameMa = ticketItem.ticketNameMa,
                ticketNameTa = ticketItem.ticketNameTa,
                ticketNameTe = ticketItem.ticketNameTe,
                ticketNameKa = ticketItem.ticketNameKa,
                ticketNameHi = ticketItem.ticketNameHi,
                ticketNamePa = ticketItem.ticketNamePa,
                ticketNameMr = ticketItem.ticketNameMr,
                ticketNameSi = ticketItem.ticketNameSi,
                ticketCategoryId = ticketItem.ticketCategoryId,
                ticketCompanyId = ticketItem.ticketCompanyId,
                ticketAmount = ticketItem.ticketAmount,
                ticketTotalAmount = ticketItem.ticketAmount,
                ticketCreatedDate = "",
                ticketCreatedBy = 1,
                ticketActive = true,
                daName = binding.editName?.text.toString(),
                daRate = ticketItem.ticketAmount,
                daQty = 1,
                daTotalAmount = ticketItem.ticketAmount,
                daPhoneNumber = binding.editPhNo?.text.toString(),
                daCustRefNo = "",
                daNpciTransId = "",
                daProofId = binding.editId?.text.toString(),
                daProof = selectedProofMode,
                daImg = byteArrayOf()
            )

            ticketRepository.insertCartBookingItem(cartItem, "Add")

            val updatedMap = ticketRepository.getTicketsMapByIds()
            ticketAdapter.updateDbItemsMap(updatedMap)

            getCartTicket()
        }
    }


    override fun onDeleteClick(ticket: Ticket) {
        TODO("Not yet implemented")
    }

    override fun onEditClick(ticket: Ticket) {
        TODO("Not yet implemented")
    }

    override fun onBackPressed() {
        val intent = Intent(this, LanguageActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}