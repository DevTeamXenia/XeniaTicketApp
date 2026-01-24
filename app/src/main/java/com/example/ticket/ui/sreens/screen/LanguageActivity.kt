package com.example.ticket.ui.sreens.screen

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
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
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.ticket.R
import retrofit2.HttpException
import com.example.ticket.data.listeners.InactivityHandlerActivity
import com.example.ticket.data.repository.CompanyRepository
import com.example.ticket.data.repository.TicketRepository
import com.example.ticket.databinding.ActivitySelectionBinding
import com.example.ticket.ui.dialog.CustomInactivityDialog
import com.example.ticket.ui.dialog.CustomInternetAvailabilityDialog
import com.example.ticket.ui.sreens.billing.BillingTicketActivity
import com.example.ticket.utils.common.ApiResponseHandler
import com.example.ticket.utils.common.CommonMethod.dismissLoader
import com.example.ticket.utils.common.CommonMethod.showLoader
import com.example.ticket.utils.common.CommonMethod.showSnackbar
import com.example.ticket.utils.common.CompanyKey
import com.example.ticket.utils.common.Constants.LANGUAGE_ENGLISH
import com.example.ticket.utils.common.Constants.LANGUAGE_HINDI
import com.example.ticket.utils.common.Constants.LANGUAGE_KANNADA
import com.example.ticket.utils.common.Constants.LANGUAGE_MALAYALAM
import com.example.ticket.utils.common.Constants.LANGUAGE_MARATHI
import com.example.ticket.utils.common.Constants.LANGUAGE_PUNJABI
import com.example.ticket.utils.common.Constants.LANGUAGE_SINHALA
import com.example.ticket.utils.common.Constants.LANGUAGE_TAMIL
import com.example.ticket.utils.common.Constants.LANGUAGE_TELUGU
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
            val isLandscape =
                resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

            val fileName = if (isLandscape) {
                companyRepository.getString(CompanyKey.COMPANYLOGO_L)
                    ?: companyRepository.getString(CompanyKey.COMPANYLOGO_P)
            } else {
                companyRepository.getString(CompanyKey.COMPANYLOGO_P)
                    ?: companyRepository.getString(CompanyKey.COMPANYLOGO_L)
            }

            if (fileName.isNullOrBlank()) return@launch

            val fullUrl = "https://apiimage.xeniapos.com/Temple/assest/uploads?fileName=$fileName"

            Glide.with(binding.imgBackground)
                .load(fullUrl)
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
//        showLoader(this@LanguageActivity, "Loading Company Settings...")

        lifecycleScope.launch {
            try {
                val token = sessionManager.getToken()
                if (token.isNullOrEmpty()) {
                    showSnackbar(binding.root, "Invalid session. Please login again.")
                    dismissLoader()
                    return@launch
                }
                val loadResult = withContext(Dispatchers.IO) {
                    companyRepository.loadCompanySettings(token)
                }
                if (!loadResult) {
                    showSnackbar(binding.root, "Company settings sync failed, using local data")
                }

                val company = companyRepository.getCompany()
                if (company == null) {
                    showSnackbar(binding.root, "Company data missing in database!")
                    dismissLoader()
                    return@launch
                }

                // setup UI sequentially
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
                        .setMessage("You have been logged out because your account was used on another device.")
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
                startActivity(Intent(this@LanguageActivity, BillingTicketActivity::class.java))
                finish()

            } else {
                sessionManager.saveSelectedLanguage(language)
                startActivity(Intent(this@LanguageActivity, TicketActivity::class.java))
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                ticketRepository.clearAllData()

                val gateway = companyRepository.getGateway()

                withContext(Dispatchers.Main) {
                    setupBackgroundImage()
                    loadCompanyDetails()

                    if (!gateway.isNullOrEmpty()) {
                        setBankLogo()
                    }
                }

            } catch (e: Exception) {
                Log.e("LanguageActivity", "Error in onResume: ${e.message}", e)
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
}