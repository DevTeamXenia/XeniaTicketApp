package com.xenia.ticket.ui.screens.kiosk

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.xenia.ticket.R
import retrofit2.HttpException
import com.xenia.ticket.data.listeners.InactivityHandlerActivity
import com.xenia.ticket.data.network.sync.InitialSyncManager
import com.xenia.ticket.data.network.sync.SyncResult
import com.xenia.ticket.data.repository.CompanySettingsRepository
import com.xenia.ticket.data.repository.OrderRepository
import com.xenia.ticket.databinding.ActivitySelectionBinding
import com.xenia.ticket.ui.dialog.CustomInactivityDialog
import com.xenia.ticket.ui.dialog.CustomInternetAvailabilityDialog
import com.xenia.ticket.ui.screens.billing.BillingTicketActivity
import com.xenia.ticket.utils.common.ApiResponseHandler
import com.xenia.ticket.utils.common.CommonMethod.showSnackbar
import com.xenia.ticket.utils.common.CompanyKey
import com.xenia.ticket.utils.common.Constants.LANGUAGE_ENGLISH
import com.xenia.ticket.utils.common.Constants.LANGUAGE_HINDI
import com.xenia.ticket.utils.common.Constants.LANGUAGE_KANNADA
import com.xenia.ticket.utils.common.Constants.LANGUAGE_MALAYALAM
import com.xenia.ticket.utils.common.Constants.LANGUAGE_MARATHI
import com.xenia.ticket.utils.common.Constants.LANGUAGE_PUNJABI
import com.xenia.ticket.utils.common.Constants.LANGUAGE_SINHALA
import com.xenia.ticket.utils.common.Constants.LANGUAGE_TAMIL
import com.xenia.ticket.utils.common.Constants.LANGUAGE_TELUGU
import com.xenia.ticket.utils.common.SessionManager
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import kotlin.collections.forEach
import kotlin.collections.get
import kotlin.getValue


class LanguageActivity : AppCompatActivity(),
    CustomInternetAvailabilityDialog.InternetAvailabilityListener,
    CustomInactivityDialog.InactivityCallback,
    InactivityHandlerActivity {
    private lateinit var binding: ActivitySelectionBinding
    private val sessionManager: SessionManager by inject()
    private val companyRepository: CompanySettingsRepository by inject()
    private var screen: String? = null
    private val ticketRepository: OrderRepository by inject()
    private val initialSyncManager: InitialSyncManager by inject()
    private var enabledLanguages: List<String> = emptyList()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        screen = intent.getStringExtra("screen")
        binding.swipeRefreshLayout.setOnRefreshListener {
            refreshAllApis()
        }
        lifecycleScope.launch {
            ticketRepository.clearAllData()
        }

        requestOverlayPermission()
        loadCompanyDetails()
    }

    override fun onResume() {
        lifecycleScope.launch {
            ticketRepository.clearAllData()
            val gateway = companyRepository.getGateway()
            if (!gateway.isNullOrEmpty()) {
                setBankLogo()
            }

        }
        super.onResume()
    }

    override fun onRestart() {
        lifecycleScope.launch {
            ticketRepository.clearAllData()
        }
        super.onRestart()
    }


    private fun setupBackgroundImage() {
        lifecycleScope.launch {
            val isLandscape =
                resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

            val imageUrl = if (isLandscape) {
                companyRepository.getString(CompanyKey.COMPANYLOGO_L)
            } else {
                companyRepository.getString(CompanyKey.COMPANYLOGO_P)
            }

            if (imageUrl.isNullOrBlank()) return@launch

            Glide.with(binding.imgBackground)
                .load(imageUrl)
                .into(binding.imgBackground)
        }
    }
    private fun getLanguageCardMap(): Map<String, View> {
        return mapOf(
            LANGUAGE_ENGLISH to binding.cardEnglish,
            LANGUAGE_MALAYALAM to binding.cardMalayalam,
            LANGUAGE_TAMIL to binding.cardTamil,
            LANGUAGE_KANNADA to binding.cardKannada,
            LANGUAGE_TELUGU to binding.cardTelugu,
            LANGUAGE_HINDI to binding.cardHindi,
            LANGUAGE_PUNJABI to binding.cardPunjabi,
            LANGUAGE_MARATHI to binding.cardMarathi,
            LANGUAGE_SINHALA to binding.cardSinhala
        ).filterValues { true }
            .mapValues { it.value }
    }
    private fun setupLanguageButtons(enabledLanguages: List<String>) {

        val languageCardMap = getLanguageCardMap()

        languageCardMap.values.forEach { it.visibility = View.GONE }

        binding.cardEnglish.apply {
            visibility = View.VISIBLE
            setOnClickListener { selectLanguage(LANGUAGE_ENGLISH) }
        }

        enabledLanguages.forEach { lang ->
            languageCardMap[lang]?.let { card ->
                card.visibility = View.VISIBLE
                card.setOnClickListener { selectLanguage(lang) }
            }
        }
    }
    private suspend fun setupCardPosition(enabledLanguages: List<String>) {
        val container = binding.languageCardContainer
        if (container.childCount < 5) return
        val languageCardMap = getLanguageCardMap()
        val defaultLanguage = companyRepository.getDefaultLanguage()

        val orderedCards = mutableListOf<View>()

        binding.cardEnglish.let { orderedCards.add(it) }

        if (defaultLanguage != LANGUAGE_ENGLISH) {
            languageCardMap[defaultLanguage]?.let { orderedCards.add(it) }
        }
        enabledLanguages.forEach { lang ->
            languageCardMap[lang]?.let { card ->
                if (card !in orderedCards) orderedCards.add(card)
            }
        }
        val rows = (0 until 5).mapNotNull {
            container.getChildAt(it) as? LinearLayout
        }
        if (rows.size < 5) return
        rows.forEach { it.removeAllViews() }
        orderedCards.forEachIndexed { index, card ->
            (card.parent as? LinearLayout)?.removeView(card)
            rows[index / 2].addView(card)
        }
        rows.forEach { row ->
            if (row.childCount == 1) {
                row.addView(
                    View(this).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1f
                        )
                    }
                )
            }
        }
    }
    private fun loadCompanyDetails() {
        lifecycleScope.launch {
            try {
                val token = sessionManager.getToken()
                if (token.isNullOrEmpty()) {
                    showSnackbar(binding.root, "Invalid session. Please login again.")
                    return@launch
                }

                val company = companyRepository.getCompany()
                if (company == null) {
                    showSnackbar(binding.root, "Company data missing in database!")
                    return@launch
                }
                val gateway = companyRepository.getString(CompanyKey.PAYMENT_GATEWAY)

                if (gateway.equals("PineLabs", ignoreCase = true)) {

                    val appId = company.applicationId

                    if (!appId.isNullOrEmpty()) {

                        sessionManager.savePineLabsAppId(appId)
                        Log.e("APP_ID_CHECK", "AppId = ${sessionManager.getPineLabsAppId()}")
                        Log.e("PINE_CONFIG", "Saved PineLabs AppId: $appId")
                    }
                }

                enabledLanguages = companyRepository
                    .getString(CompanyKey.COMPANY_LANGUAGES)
                    ?.split(",")
                    ?.map { it.trim() }
                    ?: emptyList()

                setupBackgroundImage()
                setupLanguageButtons(enabledLanguages)
                setupCardPosition(enabledLanguages)

            } catch (e: HttpException) {
                if (e.code() == 401) {
                    AlertDialog.Builder(this@LanguageActivity)
                        .setTitle("Logout !!")
                        .setMessage(
                            "You have been logged out because your account was used on another device."
                        )
                        .setCancelable(false)
                        .setPositiveButton("Logout") { _, _ ->
                            ApiResponseHandler.logoutUser(this@LanguageActivity)
                        }
                        .show()
                } else {
                    showSnackbar(binding.root, "Unable to load settings!")
                }
            } catch (e: Exception) {
                Log.e("loadCompanyDetails", "Other exception caught", e)
                showSnackbar(binding.root, "Unable to load settings!")
            } finally {
            }
        }
    }

    private fun selectLanguage(language: String) {
        lifecycleScope.launch {
            if (UserType.fromValue(sessionManager.getUserType()) == UserType.COUNTER_USER) {
                sessionManager.saveBillingSelectedLanguage(language)
                startActivity(Intent(this@LanguageActivity, BillingTicketActivity::class.java))

            } else {
                sessionManager.saveSelectedLanguage(language)
                startActivity(Intent(this@LanguageActivity, TicketActivity::class.java))
            }
        }
    }


    private suspend fun setBankLogo() {
        val gateway = companyRepository.getString(CompanyKey.PAYMENT_GATEWAY)
        val logo = when (gateway) {
            "FederalBank" -> R.drawable.ic_fed
            "CanaraBank" -> R.drawable.ic_can
            else -> R.drawable.ic_sib
        }
        val drawable = ContextCompat.getDrawable(this@LanguageActivity, logo)
        drawable?.let {
            binding.bankLogo.setImageDrawable(it)
        }
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
    private fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                "package:$packageName".toUri()
            )
            overlayPermissionLauncher.launch(intent)
        }
    }
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* No action needed after permission request */ }

    override fun onRetryClicked() {
        loadCompanyDetails()
    }

    override fun onDialogInactive() {
        TODO("Not yet implemented")
    }

    override fun resetInactivityTimer() {
        lifecycleScope.launch {

        }
    }
    private fun refreshAllApis() {
        lifecycleScope.launch {
            binding.swipeRefreshLayout.isRefreshing = true
            val result = initialSyncManager.startInitialLoad()
            binding.swipeRefreshLayout.isRefreshing = false
            val company = companyRepository.getCompany()
            company?.applicationId?.let { newAppId ->
                sessionManager.clearPineLabsAppId()
                sessionManager.savePineLabsAppId(newAppId)
            }
            when (result) {
                is SyncResult.Success -> {
                    showSnackbar(binding.root, "Data Refreshed")
                    loadCompanyDetails()
                }

                is SyncResult.Error -> {
                    val errorMessage = result.message.ifEmpty { "Unknown sync error" }
                    val code = result.code // Int?

                    Log.e("SYNC_ERROR", "Sync failed: $errorMessage, HTTP code: ${code ?: "N/A"}")

                    if (code == null || code == 401 || code == 403) {
                        AlertDialog.Builder(this@LanguageActivity)
                            .setTitle("Logout !!")
                            .setMessage(
                                "You have been logged out because your account was used on another device."
                            )
                            .setCancelable(false)
                            .setPositiveButton("Logout") { _, _ ->
                                ApiResponseHandler.logoutUser(this@LanguageActivity)
                            }
                            .show()
                    } else {
                        // Show retry dialog for other errors
                        showRetryDialog(errorMessage)
                    }
                }
            }
        }
    }
    private fun showRetryDialog(msg: String) {
        AlertDialog.Builder(this)
            .setTitle("Sync Failed")
            .setMessage(msg)
            .setCancelable(false)
            .setPositiveButton("Retry") { _, _ ->
                recreate()
            }
            .setNegativeButton("Exit") { _, _ ->
                finish()
            }
            .show()
    }

}