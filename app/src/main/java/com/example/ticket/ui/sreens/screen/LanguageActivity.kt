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
import android.widget.RelativeLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.ticket.R
import com.example.ticket.data.enum.UserType
import com.example.ticket.data.listeners.InactivityHandlerActivity
import com.example.ticket.data.repository.CompanyRepository
import com.example.ticket.databinding.ActivityLanguageBinding
import com.example.ticket.databinding.ActivityMainBinding
import com.example.ticket.ui.dialog.CustomInactivityDialog
import com.example.ticket.ui.dialog.CustomInternetAvailabilityDialog
import com.example.ticket.ui.sreens.billing.Billing_Selection_Activity
import com.example.ticket.utils.common.CommonMethod.dismissLoader
import com.example.ticket.utils.common.CommonMethod.getScreenSize
import com.example.ticket.utils.common.CommonMethod.isLandscapeScreen
import com.example.ticket.utils.common.CommonMethod.showSnackbar
import com.example.ticket.utils.common.CompanyKey
import com.example.ticket.utils.common.Constants.LANGUAGE_ENGLISH
import com.example.ticket.utils.common.Constants.LANGUAGE_HINDI
import com.example.ticket.utils.common.Constants.LANGUAGE_KANNADA
import com.example.ticket.utils.common.Constants.LANGUAGE_MALAYALAM
import com.example.ticket.utils.common.Constants.LANGUAGE_MARATHI
import com.example.ticket.utils.common.Constants.LANGUAGE_PUNJABI
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

class LanguageActivity : AppCompatActivity(),
    CustomInternetAvailabilityDialog.InternetAvailabilityListener,
    CustomInactivityDialog.InactivityCallback,
    InactivityHandlerActivity {
    private lateinit var binding: ActivityLanguageBinding
    private val sessionManager: SessionManager by inject()
    private val companyRepository: CompanyRepository by inject()
    private var screen: String? = null

    private lateinit var inactivityHandler: InactivityHandler
    private lateinit var inactivityDialog: CustomInactivityDialog

    private var enabledLanguages: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLanguageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getScreenInfo(applicationContext)
        screen = intent.getStringExtra("screen")


        inactivityDialog = CustomInactivityDialog(this)

        inactivityHandler = InactivityHandler(
            this@LanguageActivity,
            supportFragmentManager,
            inactivityDialog
        )

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

    private fun getScreenInfo(context: Context) {
        val screenSize = getScreenSize(context)
        if (screenSize == "Large" || screenSize == "Normal" || screenSize == "Small" && !isLandscapeScreen(
                context
            )
        ) {
            val topParams =
                findViewById<RelativeLayout>(R.id.layoutTop).layoutParams as LinearLayout.LayoutParams
            val middleParams =
                findViewById<LinearLayout>(R.id.layoutMiddle).layoutParams as LinearLayout.LayoutParams
            val bottomParams =
                findViewById<LinearLayout>(R.id.layoutBottom).layoutParams as LinearLayout.LayoutParams

            topParams.weight = 40F
            middleParams.weight = 53F
            bottomParams.weight = 7F
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
        binding.cardEnglish.setOnClickListener {
            Log.d("LanguageActivity", "English card clicked!")
            selectLanguage(LANGUAGE_ENGLISH)
        }

        enabledLanguages.forEach { lang ->
            languageCardMap[lang]?.apply {
                setOnClickListener {
                    Log.d("LanguageActivity", "$lang card clicked!")
                    selectLanguage(lang)
                }
            }
        }

        enabledLanguages.forEach { lang ->
            languageCardMap[lang]?.apply {
                visibility = View.VISIBLE
                setOnClickListener { selectLanguage(lang) }
            }
        }
    }

    private suspend fun setupCardPosition(enabledLanguages: List<String>) {

        val languageCardMap = getLanguageCardMap()
        val defaultLanguage = companyRepository.getDefaultLanguage()

        val orderedCards = mutableListOf<View>()
        orderedCards.add(binding.cardEnglish)

        if (defaultLanguage != LANGUAGE_ENGLISH) {
            languageCardMap[defaultLanguage]?.let {
                orderedCards.add(it)
            }
        }

        enabledLanguages.forEach { lang ->
            languageCardMap[lang]?.let { card ->
                if (card !in orderedCards) {
                    orderedCards.add(card)
                }
            }
        }

        val container = binding.languageCardContainer
        val rowCount = container.childCount

        if (rowCount == 0) return

        val rows = (0 until rowCount).mapNotNull {
            container.getChildAt(it) as? LinearLayout
        }

        rows.forEach { it.removeAllViews() }

        orderedCards.forEachIndexed { index, card ->
            val rowIndex = index / 2
            if (rowIndex < rows.size) {
                (card.parent as? LinearLayout)?.removeView(card)
                rows[rowIndex].addView(card)
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
                    Intent(this@LanguageActivity, Billing_Selection_Activity::class.java)
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
                        Intent(this@LanguageActivity, SelectionActivity::class.java)
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
        inactivityHandler.resumeInactivityCheck()


    }

    override fun onPause() {
        super.onPause()
        inactivityHandler.pauseInactivityCheck()
    }


    override fun onDestroy() {
        super.onDestroy()
            inactivityHandler.cleanup()

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
            inactivityHandler.cleanup()

        }
    }


}