package com.xenia.ticket.ui.screens.kiosk

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
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
import com.xenia.ticket.data.network.local.InitialSyncManager
import com.xenia.ticket.data.network.local.SyncResult
import com.xenia.ticket.data.repository.CompanyRepository
import com.xenia.ticket.data.repository.TicketRepository
import com.xenia.ticket.databinding.ActivitySelectionBinding
import com.xenia.ticket.ui.dialog.CustomInactivityDialog
import com.xenia.ticket.ui.dialog.CustomInternetAvailabilityDialog
import com.xenia.ticket.ui.screens.billing.BillingTicketActivity
import com.xenia.ticket.utils.common.ApiResponseHandler
import com.xenia.ticket.utils.common.CommonMethod.dismissLoader
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
import com.xenia.ticket.utils.common.JwtUtils
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
    private val companyRepository: CompanyRepository by inject()
    private var screen: String? = null
    private val ticketRepository: TicketRepository by inject()
    private val initialSyncManager: InitialSyncManager by inject()
    private var enabledLanguages: List<String> = emptyList()
    private var isSubscriptionDialogShown = false


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
            if (!isSubscriptionDialogShown) {
                lifecycleScope.launch {
                    sessionManager.getToken()?.let { token ->
                        showSubscriptionDialog(token)
                        isSubscriptionDialogShown = true
                    }
                }
            }
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
                    ?: companyRepository.getString(CompanyKey.COMPANYLOGO_P)
            } else {
                companyRepository.getString(CompanyKey.COMPANYLOGO_P)
                    ?: companyRepository.getString(CompanyKey.COMPANYLOGO_L)
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
                    dismissLoader()
                    return@launch
                }

                val company = companyRepository.getCompany()
                if (company == null) {
                    showSnackbar(binding.root, "Company data missing in database!")
                    dismissLoader()
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
                dismissLoader()
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
    private fun showSubscriptionDialog(token: String) {

        val companyName = JwtUtils.getCompanyName(token)
        val daysRemaining = JwtUtils.getRemainingDays(token)

        if (companyName.isNullOrEmpty() || daysRemaining == null) return
        if (daysRemaining > 15) return

        val dialogView = layoutInflater.inflate(R.layout.dialog_subscription, null)

        val txtMessage = dialogView.findViewById<TextView>(R.id.txtMessage)
        val txtCompany = dialogView.findViewById<TextView>(R.id.txtCompany)
        val btnLogout = dialogView.findViewById<Button>(R.id.btnRenew)
        val btnPaid = dialogView.findViewById<Button>(R.id.btnPaid)

        txtCompany.text = "Company : $companyName"

        val userType = JwtUtils.getUserType(token)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.setCanceledOnTouchOutside(false)

        dialog.setOnShowListener {
            dialog.window?.clearFlags(
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            )
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val userTypeEnum =
            UserType.values().find { it.value.equals(userType, true) }
                ?: UserType.UNKNOWN
        fun dismissDialogImmediately() {

            btnPaid.isEnabled = false
            btnLogout.isEnabled = false

            dialog.dismiss()
        }

        if (userTypeEnum == UserType.COUNTER_USER) {

            btnLogout.visibility = View.VISIBLE
            btnLogout.text = "LOGOUT"
            btnLogout.setOnClickListener {
                dialog.dismiss()
                ApiResponseHandler.logoutUser(this)
            }

            btnPaid.visibility = View.VISIBLE
            btnPaid.text = if (daysRemaining <= 0) "RENEW NOW" else "SKIP NOW"

            btnPaid.setOnClickListener {
                dismissDialogImmediately()
            }

        } else {

            if (daysRemaining <= 0) {

                btnLogout.visibility = View.VISIBLE
                btnLogout.text = "LOGOUT"
                btnLogout.setOnClickListener {
                    dialog.dismiss()
                    ApiResponseHandler.logoutUser(this)
                }

                btnPaid.visibility = View.GONE

            } else {

                btnLogout.visibility = View.GONE

                btnPaid.visibility = View.VISIBLE
                btnPaid.text = "SKIP NOW"

                btnPaid.setOnClickListener {
                    dismissDialogImmediately()
                }
            }
        }

        txtMessage.text =
            if (daysRemaining <= 0) {
                "Your subscription has expired. Please renew licence to continue."
            } else {
                "Your subscription will expire in $daysRemaining days. Please renew to avoid interruption."
            }

        dialog.show()
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
            binding.swipeRefreshLayout?.isRefreshing = true

            val result = initialSyncManager.startInitialLoad()

            binding.swipeRefreshLayout?.isRefreshing = false
            val company = companyRepository.getCompany()
            company?.applicationId?.let { newAppId->
                sessionManager.clearPineLabsAppId()
                sessionManager.savePineLabsAppId(newAppId.toString())
            }
            when (result) {
                is SyncResult.Success -> {
                    showSnackbar(binding.root, "Data Refreshed")
                    loadCompanyDetails()
                }
                is SyncResult.Error -> {
                    showSnackbar(binding.root, "Sync failed")
                }
            }
        }
    }


}