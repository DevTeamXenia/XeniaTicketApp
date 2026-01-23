package com.example.ticket.ui.sreens.billing

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ticket.R
import com.example.ticket.data.listeners.OnBookingTicketClick
import com.example.ticket.data.listeners.OnTicketClickListener
import com.example.ticket.data.network.model.TicketDto
import com.example.ticket.data.repository.ActiveTicketRepository
import com.example.ticket.data.repository.CategoryRepository
import com.example.ticket.data.repository.CompanyRepository
import com.example.ticket.data.repository.LabelSettingsRepository
import com.example.ticket.data.repository.TicketRepository
import com.example.ticket.data.room.entity.ActiveTicket
import com.example.ticket.data.room.entity.Category
import com.example.ticket.data.room.entity.LabelSettings
import com.example.ticket.data.room.entity.Ticket
import com.example.ticket.databinding.ActivityBillinTicketBinding
import com.example.ticket.ui.adapter.CategoryAdapter
import com.example.ticket.ui.adapter.TicketBookingAdapter
import com.example.ticket.ui.adapter.TicketCartAdapter
import com.example.ticket.ui.dialog.CustomLogoutPopupDialog
import com.example.ticket.ui.dialog.CustomTicketPopupDialogue
import com.example.ticket.ui.sreens.screen.LanguageActivity
import com.example.ticket.ui.sreens.screen.LoginActivity
import com.example.ticket.ui.sreens.screen.PrinterSettingActivity
import com.example.ticket.utils.common.ApiResponseHandler
import com.example.ticket.utils.common.CommonMethod.getScreenSize
import com.example.ticket.utils.common.CommonMethod.isLandscapeScreen
import com.example.ticket.utils.common.CommonMethod.setLocale
import com.example.ticket.utils.common.CommonMethod.showSnackbar
import com.example.ticket.utils.common.CompanyKey
import com.example.ticket.utils.common.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.koin.android.ext.android.inject
import java.util.Locale
import retrofit2.HttpException
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
    private val labelSettingsRepository: LabelSettingsRepository by inject()

    private lateinit var categoryAdapter: CategoryAdapter
    private val customTicketPopupDialogue: CustomTicketPopupDialogue by inject()
    private lateinit var ticketAdapter: TicketBookingAdapter
    private var selectedProofMode: String = ""
    private var selectedLanguage: String? = ""
    private var selectedCategoryId: Int = 0

    private var reportsExpanded = false
    private lateinit var ticketItemsItems: TicketDto
    private var formattedTotalAmount: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBillinTicketBinding.inflate(layoutInflater)

        if (intent.getBooleanExtra("forceLandscape", false)) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        setContentView(binding.root)


        selectedLanguage = sessionManager.getBillingSelectedLanguage()
        setLocale(this, selectedLanguage)
        setupUI()
        fetchDetails()
        setContentView(binding.root)
        setupRecyclerViews()
        setupListener()
        lifecycleScope.launch {
            updateCartUI()
        }

        getLabel()
    }
    private fun setupUI() {
        binding.txtHome?.text = getString(R.string.home)
        binding.txtselectTicket.text = getString(R.string.choose_your_tickets)
        binding.btnProceed.text = getString(R.string.proceed)
        val menu = binding.navView.menu
        menu.findItem(R.id.nav_language).title = getString(R.string.language_settings)
        menu.findItem(R.id.nav_logout).title = getString(R.string.logout)
        val headerView = binding.navView.getHeaderView(0)
        val txtTop = headerView.findViewById<TextView>(R.id.txtTop)
        txtTop.text = getString(R.string.dashboard)
        binding.btnProceed.setOnClickListener {
            val intent = Intent(applicationContext, Billing_Cart_Activity::class.java)

            startActivity(intent)
        }
        binding.imgMenu.setOnClickListener {
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                binding.drawerLayout.openDrawer(GravityCompat.START)
            }
        }
    }
    private fun getLabel() {
        lifecycleScope.launch {

            labelSettingsRepository.loadLabelSettings(sessionManager.getToken().toString())

            repeatOnLifecycle(Lifecycle.State.STARTED) {
                labelSettingsRepository
                    .getLabelSettingsFromDb()
                    .collect { labels ->

                        val ticketLabel = labels.find {
                            it.settingKey.equals("ticket", ignoreCase = true)
                        }

                        ticketLabel?.let {
                            binding.texthead1.text =
                                it.getDisplayNameByLanguage(this@Billin_Ticket_Activity)
                        }
                    }
            }
        }
    }
    fun LabelSettings.getDisplayNameByLanguage(context: Context): String {
        val lang = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0].language
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale.language
        }
        val display = when (lang) {
            "ml" -> displayNameMa
            "hi" -> displayNameHi
            "ta" -> displayNameTa
            "te" -> displayNameTe
            "kn" -> displayNameKa
            "pa" -> displayNamePa
            "mr" -> displayNameMr
            "si" -> displayNameSi
            else -> displayName
        }

        return display?.takeIf { it.isNotBlank() } ?: displayName
    }
    private fun setupListener() {

        binding.navView.setNavigationItemSelectedListener { item ->
            val menu = binding.navView.menu

            when (item.itemId) {
                R.id.nav_language -> {
                    startActivity(Intent(applicationContext, LanguageActivity::class.java))
                    finish()
                    false
                }
                R.id.nav_settings -> {
                    startActivity(Intent(applicationContext, PrinterSettingActivity::class.java))
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }

                R.id.nav_reports -> {
                    reportsExpanded = !reportsExpanded
                    menu.findItem(R.id.nav_detailed).isVisible = reportsExpanded
                    menu.findItem(R.id.nav_summary).isVisible = reportsExpanded
                    true
                }

                R.id.nav_detailed -> {

                    startActivity(Intent(applicationContext, DetailedReportActivity::class.java))
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }

                R.id.nav_summary -> {

                    startActivity(Intent(applicationContext, SummaryReportActivity::class.java))
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_logout -> {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)

                    val customPopupDialog = CustomLogoutPopupDialog()
                    customPopupDialog.show(supportFragmentManager, "Logout")

                    true
                }


                else -> {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
            }
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

    }

    private fun fetchDetails() {
        lifecycleScope.launch {
            val isCategoryEnabled = companyRepository.getString(CompanyKey.CATEGORY_ENABLE)?.toBoolean() ?: false
            if (isCategoryEnabled) {
                getCategory()
            } else {
                getTickets()
            }
        }
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
            } catch (e: HttpException) {
                if (e.code() == 401) {
                    val errorBody = e.response()?.errorBody()?.string().orEmpty()
                    val message = try { JSONObject(errorBody).optString("message") } catch (ex: Exception) { null }
                    val displayMessage = message?.ifBlank { "Your session has expired. Please login again." }

                    // Show dialog safely
                    runOnUiThread {
                        AlertDialog.Builder(this@Billin_Ticket_Activity)
                            .setTitle("Session Expired")
                            .setMessage(displayMessage)
                            .setCancelable(false)
                            .setPositiveButton("Logout") { _, _ ->
                                sessionManager.clearSession()
                                startActivity(
                                    Intent(this@Billin_Ticket_Activity, LoginActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                                Intent.FLAG_ACTIVITY_NEW_TASK or
                                                Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    }
                                )
                                finish()
                            }
                            .show()
                    }
                } else {
                    throw e
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }



    private fun getTickets(categoryId: Int? = null) {
        lifecycleScope.launch {
            try {
                val token = sessionManager.getToken()
                if (token.isNullOrEmpty()) return@launch

                try {
                    activeTicketRepository.loadTickets(token)
                } catch (e: Exception) {
                    Log.w("TICKET_SYNC", "Ticket sync failed, using local data", e)
                }
                val tickets = if (categoryId != null) {
                    activeTicketRepository.getTicketsByCategory(categoryId)
                } else {
                    activeTicketRepository.getAllTickets()
                }

                if (tickets.isEmpty()) {
                    ticketAdapter.updateTickets(emptyList())
                    showSnackbar(binding.root, "No tickets available")
                    return@launch
                }

                val ticketDtos = tickets.map { it.toDto() }
                ticketAdapter.updateTickets(ticketDtos)

            } catch (e: Exception) {
                showSnackbar(binding.root, "Error loading tickets")
                Log.e("TICKET_LOAD", "Error", e)
            }
        }
    }

    override fun onCategoryClick(category: Category) {
        selectedCategoryId = category.categoryId
        getTickets(selectedCategoryId) // only reload tickets for clicked category
    }

    private fun getTickets() {
        lifecycleScope.launch {
            try {
                val token = sessionManager.getToken()
                if (token.isNullOrEmpty()) return@launch
                try {
                    activeTicketRepository.loadTickets(token)
                } catch (e: Exception) {
                    Log.w("TICKET_SYNC", "Ticket sync failed, using local data", e)
                }
                val tickets = activeTicketRepository.getAllTickets()
                if (tickets.isEmpty()) {
                    ticketAdapter.updateTickets(emptyList())
                    return@launch
                }
                val ticketDtos = tickets.map { it.toDto() }
                ticketAdapter.updateTickets(ticketDtos)

            } catch (e: Exception) {
                showSnackbar(binding.root, "Something went wrong: ${e.localizedMessage}")
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
            fetchDetails()
            updateCartUI()
        }
    }

    override fun onTicketAdded() {
        lifecycleScope.launch {
           fetchDetails()
            updateCartUI()
        }
    }

    override fun onRestart() {
        super.onRestart()
        setupRecyclerViews()
        fetchDetails()
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
            formattedTotalAmount = String.format(Locale.ENGLISH, "%.2f", totalAmount)
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