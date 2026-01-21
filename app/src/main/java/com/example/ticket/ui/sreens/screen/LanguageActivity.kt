package com.example.ticket.ui.sreens.screen

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.ticket.R
import com.example.ticket.data.enum.UserType
import com.example.ticket.data.listeners.InactivityHandlerActivity
import com.example.ticket.data.repository.CompanyRepository
import com.example.ticket.data.repository.TicketRepository
import com.example.ticket.databinding.ActivitySelectionBinding
import com.example.ticket.ui.dialog.CustomInactivityDialog
import com.example.ticket.ui.dialog.CustomInternetAvailabilityDialog
import com.example.ticket.ui.sreens.billing.Billin_Ticket_Activity
import com.example.ticket.utils.common.CommonMethod.dismissLoader
import com.example.ticket.utils.common.CommonMethod.showSnackbar
import com.example.ticket.utils.common.CompanyKey
import com.example.ticket.utils.common.Constants.COMPANY_GATEWAY_CAN
import com.example.ticket.utils.common.Constants.COMPANY_GATEWAY_DHANLAXMI
import com.example.ticket.utils.common.Constants.LANGUAGE_ENGLISH
import com.example.ticket.utils.common.Constants.LANGUAGE_HINDI
import com.example.ticket.utils.common.Constants.LANGUAGE_KANNADA
import com.example.ticket.utils.common.Constants.LANGUAGE_MALAYALAM
import com.example.ticket.utils.common.Constants.LANGUAGE_MARATHI
import com.example.ticket.utils.common.Constants.LANGUAGE_PUNJABI
import com.example.ticket.utils.common.Constants.LANGUAGE_SINHALA
import com.example.ticket.utils.common.Constants.LANGUAGE_TAMIL
import com.example.ticket.utils.common.Constants.LANGUAGE_TELUGU
import com.example.ticket.utils.common.InactivityHandler
import com.example.ticket.utils.common.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import java.io.File
import java.io.FileOutputStream
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


    private var enabledLanguages: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        screen = intent.getStringExtra("screen")


        requestOverlayPermission()
        loadCompanyDetails()
    }
        private fun setupBackgroundImage() {
            lifecycleScope.launch {
                val fileName =
                    if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        companyRepository.getString(CompanyKey.COMPANYLOGO_L)
                    } else {
                        companyRepository.getString(CompanyKey.COMPANYLOGO_P)
                    }

                if (fileName.isNullOrEmpty()) return@launch

                val imageUrl = "https://apiimage.xeniapos.com/Temple/assest/uploads?fileName=$fileName"

                Glide.with(binding.imgBackground.context)
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
            ).filterValues { it != null }
                .mapValues { it.value!! }
        }


        private fun setupLanguageButtons(enabledLanguages: List<String>) {

            val languageCardMap = getLanguageCardMap()

            languageCardMap.values.forEach { it.visibility = View.GONE }

            binding.cardEnglish.apply {
                visibility = View.VISIBLE
                setOnClickListener { selectLanguage(LANGUAGE_ENGLISH) }
            }

            enabledLanguages.forEach { lang ->
                languageCardMap[lang]?.apply {
                    visibility = View.VISIBLE
                    setOnClickListener { selectLanguage(lang) }
                }
            }


            lifecycleScope.launch {
                setupCardPosition(enabledLanguages)
            }
        }

        private suspend fun setupCardPosition(enabledLanguages: List<String>) {

            val container = binding.languageCardContainer ?: return
            if (container.childCount < 5) return

            val languageCardMap = getLanguageCardMap()
            val defaultLanguage = companyRepository.getDefaultLanguage()

            val orderedCards = mutableListOf<View>()

            binding.cardEnglish?.let { orderedCards.add(it) }

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
                        dismissLoader()
                        showSnackbar(binding.root, "Invalid session. Please login again.")
                        return@launch
                    }

                    val isLoaded = withContext(Dispatchers.IO) {
                        companyRepository.loadCompanySettings(token)
                    }

                    if (!isLoaded) {
                        dismissLoader()
                        showSnackbar(binding.root, "Company details not found!")
                        return@launch
                    }
                    val company = withContext(Dispatchers.IO) {
                        companyRepository.getCompany()
                    }
                    if (company == null) {
                        dismissLoader()
                        showSnackbar(binding.root, "Company data missing in database!")
                        return@launch
                    }


                    enabledLanguages = companyRepository
                        .getString(CompanyKey.COMPANY_LANGUAGES)
                        ?.split(",")
                        ?.map { it.trim() }
                        ?: emptyList()

                    setupBackgroundImage()
                    setupLanguageButtons(enabledLanguages)
                    setupCardPosition(enabledLanguages)

                    dismissLoader()

                    val bgImageUrl =
                        companyRepository.getString(CompanyKey.COMPANYLOGO_L)

                    if (!bgImageUrl.isNullOrEmpty()) {
                        val bitmap = loadBitmapSafely(this@LanguageActivity, bgImageUrl)

                        bitmap?.let {
                            val file = saveBitmapToFile(
                                context = this@LanguageActivity,
                                bitmap = it,
                                filename = "company_bg.png"
                            )
                            binding.root.background =
                                Drawable.createFromPath(file.absolutePath)
                        }
                    }

                    dismissLoader()

                } catch (e: Exception) {
                    dismissLoader()
                    showSnackbar(binding.root, "Unable to load settings!")
                    e.printStackTrace()
                }
            }
        }

        private fun saveBitmapToFile(context: Context, bitmap: Bitmap, filename: String): File {
            val file = File(context.cacheDir, filename)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            return file
        }

        private suspend fun loadBitmapSafely(context: Context, url: String): Bitmap? =
            withContext(Dispatchers.IO) {
                try {
                    Glide.with(context)
                        .asBitmap()
                        .load(url)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .submit()
                        .get()
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

        private fun selectLanguage(language: String) {
            lifecycleScope.launch {

                if (UserType.fromValue(sessionManager.getUserType()) == UserType.COUNTER_USER) {

                    sessionManager.saveBillingSelectedLanguage(language)
                    startActivity(
                        Intent(this@LanguageActivity, Billin_Ticket_Activity::class.java)
                    )
                    finish()

                } else {

                    sessionManager.saveSelectedLanguage(language)

                    val categoryValue = companyRepository.getString(CompanyKey.CATEGORY_ENABLE)
                    val isCategoryEnabled = categoryValue?.let {
                        it.equals("true", ignoreCase = true) || it == "1" || it.equals(
                            "yes",
                            ignoreCase = true
                        )
                    } ?: false

                    if (isCategoryEnabled) {
                        startActivity(
                            Intent(this@LanguageActivity, TicketActivity::class.java)
                        )
                    } else {
                        // fallback if category not enabled
                        // startActivity(Intent(this@LanguageActivity, HomeActivity::class.java))
                    }

                    finish()
                }
            }
        }

    override fun onResume() {
        super.onResume()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Clear any previous ticket data
                ticketRepository.clearAllData()

                val gatewayValue = companyRepository.getGateway()

                val isPaymentGatewayActive = gatewayValue?.toBoolean()


                val logoRes = when (isPaymentGatewayActive) {
                    true -> R.drawable.ic_can
                    false -> R.drawable.ic_dhan
                    null -> R.drawable.ic_sib
                }

                withContext(Dispatchers.Main) {
                    setupBackgroundImage()
                    loadCompanyDetails()
                    setBankLogo()
                    val drawable = ContextCompat.getDrawable(this@LanguageActivity, logoRes)
                    binding.bankLogo.setImageDrawable(drawable)
                }

            } catch (e: Exception) {
                Log.e("LanguageActivity", "Error in onResume: ${e.message}", e)
            }
        }
    }

    private suspend fun setBankLogo() {
        val gateway = withContext(Dispatchers.IO) {
            companyRepository.getGateway()
        }
        val logo = when (gateway) {
            COMPANY_GATEWAY_CAN -> R.drawable.ic_can
            COMPANY_GATEWAY_DHANLAXMI -> R.drawable.ic_dhan
            else -> R.drawable.ic_sib
        }
        val drawable = ContextCompat.getDrawable(this@LanguageActivity, logo)
        if (drawable != null) {
            binding.bankLogo.setImageDrawable(drawable)

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

    }