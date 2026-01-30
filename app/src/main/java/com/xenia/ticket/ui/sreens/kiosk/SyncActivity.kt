package com.xenia.ticket.ui.sreens.kiosk


import UserType
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.xenia.ticket.R
import com.xenia.ticket.data.network.local.InitialSyncManager
import com.xenia.ticket.data.network.local.SyncResult
import com.xenia.ticket.databinding.ActivitySyncBinding
import com.xenia.ticket.utils.common.CommonMethod
import com.xenia.ticket.utils.common.CommonMethod.showSnackbar
import com.xenia.ticket.utils.common.Constants.PRINTER_KIOSK
import com.xenia.ticket.utils.common.SessionManager
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import kotlin.apply
import kotlin.jvm.java


class SyncActivity : AppCompatActivity() {
    private val sessionManager: SessionManager by inject()
    private val syncManager: InitialSyncManager by inject()


    private lateinit var binding: ActivitySyncBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySyncBinding.inflate(layoutInflater)
        setContentView(binding.root)
        startSyncProcess()
    }

    private fun startSyncProcess() {
        lifecycleScope.launch {
            showLoader()
            val result = syncManager.startInitialLoad()
            dismissLoader()
            when (result) {
                is SyncResult.Success -> {
                    val userType = UserType.fromValue(
                        sessionManager.getUserType()
                    )
                    val userId = sessionManager.getUserId()
                    val sharedPref = getSharedPreferences("your_pref_name", MODE_PRIVATE)

                    val configuration = resources.configuration
                    val screenSizeMask =
                        configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK

                    val isLandscape =
                        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

                    val isPortrait =
                        configuration.orientation == Configuration.ORIENTATION_PORTRAIT

                    navigateNextScreen(
                        userType = userType,
                        screenSizeMask = screenSizeMask,
                        isLandscape = isLandscape,
                        isPortrait = isPortrait,
                        userId = userId.toString(),
                        sharedPref = sharedPref
                    )


                }
                is SyncResult.Error -> {
                    showRetryDialog(result.error)
                }
            }
        }
    }

    private fun navigateNextScreen(
        userType: UserType,
        screenSizeMask: Int,
        isLandscape: Boolean,
        isPortrait: Boolean,
        userId: String,
        sharedPref: android.content.SharedPreferences
    ) {
        lifecycleScope.launch {

            when (userType) {

                UserType.COUNTER_USER -> {

                    val allowedForLarge =
                        screenSizeMask == Configuration.SCREENLAYOUT_SIZE_LARGE
                    val allowedForNormal =
                        screenSizeMask == Configuration.SCREENLAYOUT_SIZE_NORMAL

                    if (!allowedForLarge && !allowedForNormal) {
                        CommonMethod.dismissLoader()
                        showSnackbar(
                            binding.root,
                            "COUNTER_USER has no permission for this device/orientation!"
                        )
                        return@launch
                    }

                    val isFirstLoginKey = "isFirstLogin_$userId"
                    val isFirstLogin = sharedPref.getBoolean(isFirstLoginKey, true)

                    if (isFirstLogin) {
                        openPrinterSetup()

                        sharedPref.edit {
                            putBoolean(isFirstLoginKey, false)
                        }
                    } else {
                        startActivity(Intent(this@SyncActivity, LanguageActivity::class.java))

                        finish()
                    }
                }

                UserType.PROCESS_USER -> {

                    CommonMethod.dismissLoader()
                    showSnackbar(
                        binding.root,
                        "PROCESS_USER is not allowed to login on this device!"
                    )
                    return@launch
                }

                UserType.CUSTOMER -> {

                    if (screenSizeMask != Configuration.SCREENLAYOUT_SIZE_XLARGE || !isPortrait) {
                        CommonMethod.dismissLoader()
                        startActivity(Intent(this@SyncActivity, LanguageActivity::class.java))
                    }

                    sessionManager.saveSelectedPrinter(PRINTER_KIOSK)
                    startActivity(Intent(this@SyncActivity, LanguageActivity::class.java))

                    finish()
                }

                UserType.UNKNOWN -> {
                    CommonMethod.dismissLoader()
                    showSnackbar(binding.root, "Unknown user type!")
                }
            }
        }
    }



    private fun openPrinterSetup() {
        val intent = Intent(applicationContext, PrinterSettingActivity::class.java).apply {
            putExtra("fromLogin", "Login")
        }
        startActivity(intent)
        finish()
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


    private fun showLoader() {
        binding.loaderView.visibility = View.VISIBLE
        binding.loadingText.visibility = View.VISIBLE
        binding.loadingText.text = getString(R.string.loading_syncing_data)
    }


    private fun dismissLoader() {
        binding.loaderView.visibility = View.GONE
        binding.loadingText.visibility = View.GONE
    }
}
