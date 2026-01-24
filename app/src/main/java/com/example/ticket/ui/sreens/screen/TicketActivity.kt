package com.example.ticket.ui.sreens.screen

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ticket.R
import com.example.ticket.data.listeners.InactivityHandlerActivity
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
import com.example.ticket.databinding.ActivityTicketBinding
import com.example.ticket.ui.adapter.CategoryAdapter
import com.example.ticket.ui.adapter.TicketAdapter
import com.example.ticket.ui.dialog.CustomInactivityDialog
import com.example.ticket.ui.dialog.CustomInternetAvailabilityDialog
import com.example.ticket.ui.dialog.CustomTicketPopupDialogue
import com.example.ticket.utils.common.ApiResponseHandler
import com.example.ticket.utils.common.CommonMethod.setLocale
import com.example.ticket.utils.common.CommonMethod.showSnackbar
import com.example.ticket.utils.common.CompanyKey
import com.example.ticket.utils.common.InactivityHandler
import com.example.ticket.utils.common.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import retrofit2.HttpException
import java.util.Locale
import kotlin.getValue

class TicketActivity : AppCompatActivity(), OnTicketClickListener,
    CustomInactivityDialog.InactivityCallback,CustomInternetAvailabilityDialog.InternetAvailabilityListener,
    InactivityHandlerActivity,  CategoryAdapter.OnCategoryClickListener{

    private lateinit var binding: ActivityTicketBinding
    private val ticketRepository: TicketRepository by inject()
    private val activeTicketRepository: ActiveTicketRepository by inject()
    private val categoryRepository: CategoryRepository by inject()
    private val labelSettingsRepository: LabelSettingsRepository by inject()

    private val sessionManager: SessionManager by inject()
    private val companyRepository: CompanyRepository by inject()
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var ticketAdapter: TicketAdapter
    private var categoryId: Int = 0
    private var selectedLanguage: String? = ""
    private var selectedCategoryId: Int = 0

    private var formattedTotalAmount: String = ""
    private lateinit var inactivityHandler: InactivityHandler
    private lateinit var inactivityDialog: CustomInactivityDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTicketBinding.inflate(layoutInflater)
        setLocale(this, sessionManager.getSelectedLanguage().toString())
        selectedLanguage = sessionManager.getSelectedLanguage()
        categoryId = intent.getIntExtra("CATEGORY_ID", 0)
        inactivityDialog = CustomInactivityDialog(this)
        inactivityHandler =
            InactivityHandler(this, supportFragmentManager, inactivityDialog)
        setupUI()
        setContentView(binding.root)
        setupRecyclerViews()
        fetchDetails()
        lifecycleScope.launch {
            updateCartUI()
        }
        getLabel()
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
                                it.getDisplayNameByLanguage(this@TicketActivity)
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

    private fun setupUI() {
        binding.txtHome?.text = getString(R.string.home)
        binding.txtselectTicket.text = getString(R.string.choose_your_tickets)
        binding.btnProceed.text = getString(R.string.proceed)
        binding.linHome?.setOnClickListener {
            startActivity(Intent(applicationContext, LanguageActivity::class.java))
            finish()

        }

        binding.btnProceed.setOnClickListener {
            val intent = Intent(applicationContext, TicketCartActivity::class.java)
            startActivity(intent)

        }

    }
    private fun fetchDetails() {
        lifecycleScope.launch {
            val isCategoryEnabled = companyRepository.getString(CompanyKey.CATEGORY_ENABLE)?.toBoolean() ?: false
            if (isCategoryEnabled) {
                getCategory()
            } else {
                getTickets()
                binding.linOfferCat.visibility= View.GONE
            }
        }
    }
    @SuppressLint("NotifyDataSetChanged")
    private fun setupRecyclerViews() {
        categoryAdapter = CategoryAdapter(this, selectedLanguage!!, this)

        binding.ticketCat.layoutManager = LinearLayoutManager(this)
        binding.ticketCat.adapter = categoryAdapter

        ticketAdapter = TicketAdapter(
            context = this,
            selectedLanguage = selectedLanguage ?: "en",
            coroutineScope = lifecycleScope,
            onTicketClickListener = this,
            ticketRepository = ticketRepository
        )
        binding.ticketRecycler.adapter = ticketAdapter



    }
    private fun getCategory() {
        try {
            val token = sessionManager.getToken()

            if (token.isNullOrEmpty()) {
                binding.ticketCat.visibility = View.GONE
                return
            }


            ApiResponseHandler.handleApiCall(
                activity = this@TicketActivity,
                apiCall = {
                    withContext(Dispatchers.IO) {
                        categoryRepository.loadCategories(token)
                    }
                },
                onSuccess = { isLoaded ->
                    if (!isLoaded) {
                        showSnackbar(binding.root, "Category sync failed, using local data")
                        binding.ticketCat.visibility = View.GONE
                        return@handleApiCall
                    }

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
                AlertDialog.Builder(this@TicketActivity)
                    .setTitle("Logout !!")
                    .setMessage("You have been logged out because your account was used on another device.")
                    .setCancelable(false)
                    .setPositiveButton("Logout") { _, _ ->
                        ApiResponseHandler.logoutUser(this@TicketActivity)
                    }
                    .show()

            } else {
                throw e
            }
        } catch (e: Exception) {
            showSnackbar(binding.root, "Error loading tickets")
        }
    }
    private fun getTickets(categoryId: Int? = null) {

        try {
            val token = sessionManager.getToken()
            if (token.isNullOrEmpty()) {
                return
            }

            ApiResponseHandler.handleApiCall(
                activity = this@TicketActivity,
                apiCall = {
                    withContext(Dispatchers.IO) {
                        activeTicketRepository.loadTickets(token)
                    }
                },
                onSuccess = { loadResult ->
                    if (!loadResult) {
                        showSnackbar(binding.root, "Ticket sync failed, using local data")
                    }
                    lifecycleScope.launch {
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
                    }
                    binding.ticketRecycler.layoutManager = GridLayoutManager(
                        this@TicketActivity,
                        4
                    )
                    binding.ticketRecycler.adapter = ticketAdapter
                }
            )
        } catch (e: HttpException) {
            if (e.code() == 401) {
                AlertDialog.Builder(this@TicketActivity)
                    .setTitle("Logout !!")
                    .setMessage("Your session has expired. Please login again.")
                    .setCancelable(false)
                    .setPositiveButton("Logout") { _, _ ->
                        ApiResponseHandler.logoutUser(this@TicketActivity)
                    }
                    .show()

            } else {
                throw e
            }
        } catch (e: Exception) {
            showSnackbar(binding.root, "Error loading tickets")
        }
    }

    private fun getTickets() {
        try {
            val token = sessionManager.getToken()
            if (token.isNullOrEmpty()) {
                return
            }

            ApiResponseHandler.handleApiCall(
                activity = this@TicketActivity,
                apiCall = {
                    withContext(Dispatchers.IO) {
                        activeTicketRepository.loadTickets(token)
                    }
                },
                onSuccess = { loadResult ->
                    if (!loadResult) {
                        showSnackbar(binding.root, "Ticket sync failed, using local data")
                    }
                    lifecycleScope.launch {
                        val tickets = activeTicketRepository.getAllTickets()
                        if (tickets.isEmpty()) {
                            ticketAdapter.updateTickets(emptyList())
                            return@launch
                        }
                        val ticketDtos = tickets.map { it.toDto() }
                        ticketAdapter.updateTickets(ticketDtos)
                        binding.ticketRecycler.layoutManager = GridLayoutManager(
                            this@TicketActivity,
                            4
                        )
                        binding.ticketRecycler.adapter = ticketAdapter
                    }
                }
            )
        } catch (e: HttpException) {
            if (e.code() == 401) {
                AlertDialog.Builder(this@TicketActivity)
                    .setTitle("Logout !!")
                    .setMessage("Your session has expired. Please login again.")
                    .setCancelable(false)
                    .setPositiveButton("Logout") { _, _ ->
                        ApiResponseHandler.logoutUser(this@TicketActivity)
                    }
                    .show()

            } else {
                throw e
            }
        } catch (e: Exception) {
            showSnackbar(binding.root, "Error loading tickets")
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
                        this@TicketActivity,
                        R.color.primaryColor
                    )
                )
            } else {
                binding.btnProceed.text = getString(R.string.proceed)
                binding.btnProceed.isEnabled = false
                binding.btnProceed.setBackgroundColor(
                    ContextCompat.getColor(
                        this@TicketActivity,
                        R.color.light_grey
                    )
                )
            }
        }
    }



    override fun onPause() {
        super.onPause()
        inactivityHandler.pauseInactivityCheck()
    }

    override fun onDestroy() {
        super.onDestroy()
        inactivityHandler.cleanup()
    }

    override fun onRetryClicked() {
        inactivityHandler.resumeInactivityCheck()
        //generatePaymentToken(donationAmount!!)
    }

    override fun onDialogInactive() {
        inactivityHandler.resumeInactivityCheck()
        inactivityHandler.showDialogSafely()
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        inactivityHandler.resetTimer()
        return super.dispatchTouchEvent(ev)
    }

    override fun resetInactivityTimer() {
        inactivityHandler.resetTimer()
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