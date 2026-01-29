package com.xenia.ticket.ui.sreens.billing

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
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
import com.xenia.ticket.R
import com.xenia.ticket.data.listeners.OnBookingTicketClick
import com.xenia.ticket.data.listeners.OnTicketClickListener
import com.xenia.ticket.data.network.model.TicketDto
import com.xenia.ticket.data.repository.ActiveTicketRepository
import com.xenia.ticket.data.repository.CategoryRepository
import com.xenia.ticket.data.repository.CompanyRepository
import com.xenia.ticket.data.repository.LabelSettingsRepository
import com.xenia.ticket.data.repository.TicketRepository
import com.xenia.ticket.data.room.entity.ActiveTicket
import com.xenia.ticket.data.room.entity.Category
import com.xenia.ticket.data.room.entity.LabelSettings
import com.xenia.ticket.data.room.entity.Ticket
import com.xenia.ticket.databinding.ActivityBillinTicketBinding
import com.xenia.ticket.ui.adapter.CategoryAdapter
import com.xenia.ticket.ui.adapter.TicketBookingAdapter
import com.xenia.ticket.ui.adapter.TicketCartAdapter
import com.xenia.ticket.ui.dialog.CustomLogoutPopupDialog
import com.xenia.ticket.ui.dialog.CustomTicketPopupDialogue
import com.xenia.ticket.ui.sreens.kiosk.LanguageActivity
import com.xenia.ticket.ui.sreens.kiosk.PrinterSettingActivity
import com.xenia.ticket.utils.common.ApiResponseHandler
import com.xenia.ticket.utils.common.CommonMethod.dismissLoader
import com.xenia.ticket.utils.common.CommonMethod.setLocale
import com.xenia.ticket.utils.common.CommonMethod.showLoader
import com.xenia.ticket.utils.common.CommonMethod.showSnackbar
import com.xenia.ticket.utils.common.CompanyKey
import com.xenia.ticket.utils.common.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import java.util.Locale
import retrofit2.HttpException
import kotlin.getValue

class BillingTicketActivity : AppCompatActivity(), OnTicketClickListener,
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
    private var spanCount: Int = 2
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
        binding.txtselectTicket.text = getString(R.string.choose_your_tickets)
        binding.btnProceed.text = getString(R.string.proceed)
        val menu = binding.navView.menu
        menu.findItem(R.id.nav_language).title = getString(R.string.language_settings)
        menu.findItem(R.id.nav_logout).title = getString(R.string.logout)
        val headerView = binding.navView.getHeaderView(0)
        val txtTop = headerView.findViewById<TextView>(R.id.txtTop)
        txtTop.text = getString(R.string.dashboard)
        binding.btnProceed.setOnClickListener {
            val intent = Intent(applicationContext, BillingCartActivity::class.java)

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
                                it.getDisplayNameByLanguage(this@BillingTicketActivity)
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
        binding.ticketRecycler.adapter = ticketAdapter


        binding.relCart.layoutManager = LinearLayoutManager(this)
        ticketCartAdapter = TicketCartAdapter(this, selectedLanguage!!, "Booking", this)
        binding.relCart.adapter = ticketCartAdapter

    }
    private fun fetchDetails() {
        lifecycleScope.launch {
            val isCategoryEnabled = companyRepository.getString(CompanyKey.CATEGORY_ENABLE)
            if (isCategoryEnabled == "True") {
                spanCount=2
                getCategory()
            } else {
                spanCount=2
                getTickets(selectedCategoryId)
                binding.ticketCategory.visibility= View.GONE
            }
        }
    }
    private fun getCategory() {
        try {
            val token = sessionManager.getToken()

            if (token.isNullOrEmpty()) {
                binding.ticketCat.visibility = View.GONE
                return
            }

            ApiResponseHandler.handleApiCall(
                activity = this@BillingTicketActivity,
                apiCall = {
                    true
                },
                onSuccess = {
                    lifecycleScope.launch {
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
                    }
                }
            )

        } catch (e: HttpException) {
            if (e.code() == 401) {
                AlertDialog.Builder(this@BillingTicketActivity)
                    .setTitle("Logout !!")
                    .setMessage("You have been logged out because your account was used on another device.")
                    .setCancelable(false)
                    .setPositiveButton("Logout") { _, _ ->
                        ApiResponseHandler.logoutUser(this@BillingTicketActivity)
                    }
                    .show()
            } else {
                throw e
            }
        } catch (e: Exception) {
            showSnackbar(binding.root, "Error loading categories")
        }
    }

    private fun getTickets(categoryId: Int? = null) {
        showLoader(this@BillingTicketActivity, "Loading Tickets...")
        try {
            val token = sessionManager.getToken()
            if (token.isNullOrEmpty()) return

            ApiResponseHandler.handleApiCall(
                activity = this@BillingTicketActivity,
                apiCall = {
                   true
                },
                onSuccess = { loadResult ->
                    lifecycleScope.launch {
                        try {
                            if (!loadResult) {
                                showSnackbar(binding.root, "Ticket sync failed, using local data")
                            }
                            val tickets = if (categoryId != null && categoryId != 0) {
                                activeTicketRepository.getTicketsByCategory(categoryId)
                            } else {
                                activeTicketRepository.getAllTickets()
                            }
                            if (tickets.isEmpty()) {
                                ticketAdapter.updateTickets(emptyList())
                                ticketAdapter.updateDbItemsMap(emptyMap())
                                showSnackbar(binding.root, "No tickets available")
                                return@launch
                            }
                            val dbItems = withContext(Dispatchers.IO) {
                                ticketRepository.getAllTicketsInCart().associateBy { it.ticketId }
                            }

                            val ticketDtos = tickets.map { it.toDto() }
                            ticketAdapter.updateTickets(ticketDtos)
                            ticketAdapter.updateDbItemsMap(dbItems)

                            binding.ticketRecycler.layoutManager = GridLayoutManager(
                                this@BillingTicketActivity,
                                spanCount
                            )
                            binding.ticketRecycler.adapter = ticketAdapter

                        } finally {
                            dismissLoader()
                        }
                    }
                }
            )
        } catch (e: HttpException) {
            if (e.code() == 401) {
                AlertDialog.Builder(this@BillingTicketActivity)
                    .setTitle("Logout !!")
                    .setMessage("Your session has expired. Please login again.")
                    .setCancelable(false)
                    .setPositiveButton("Logout") { _, _ ->
                        ApiResponseHandler.logoutUser(this@BillingTicketActivity)
                    }
                    .show()
            } else throw e
           dismissLoader()
        } catch (e: Exception) {
            showSnackbar(binding.root, "Error loading tickets")
            dismissLoader()
        }
    }

    override fun onCategoryClick(category: Category) {
        selectedCategoryId = category.categoryId
        getTickets(selectedCategoryId)
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

    @SuppressLint("SetTextI18n", "DefaultLocale")
    private suspend fun updateCartUI() {
        val (totalAmount, hasData) = ticketRepository.getCartStatus()

        withContext(Dispatchers.Main) {
            if (hasData) {
                formattedTotalAmount = String.format(Locale.ENGLISH, "%.2f", totalAmount)
                binding.btnProceed.text =
                    getString(R.string.proceed) + "  Rs.$formattedTotalAmount"

                binding.btnProceed.isEnabled = true
                binding.btnProceed.setBackgroundColor(
                    ContextCompat.getColor(
                        this@BillingTicketActivity,
                        R.color.primaryColor
                    )
                )
            } else {
                binding.btnProceed.text = getString(R.string.proceed)
                binding.btnProceed.isEnabled = false
                binding.btnProceed.setBackgroundColor(
                    ContextCompat.getColor(
                        this@BillingTicketActivity,
                        R.color.light_grey
                    )
                )
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


}