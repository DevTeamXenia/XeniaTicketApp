package com.xenia.ticket.ui.screens.kiosk

import UserType
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.net.toUri
import com.xenia.ticket.R
import com.xenia.ticket.data.repository.CompanyRepository
import com.xenia.ticket.data.repository.LoginRepository
import com.xenia.ticket.databinding.ActivityLoginBinding
import com.xenia.ticket.ui.screens.billing.BillingTicketActivity
import com.xenia.ticket.utils.common.CommonMethod.dismissLoader
import com.xenia.ticket.utils.common.CommonMethod.isInternetAvailable
import com.xenia.ticket.utils.common.CommonMethod.showLoader
import com.xenia.ticket.utils.common.CommonMethod.showSnackbar
import com.xenia.ticket.utils.common.Constants.PRINTER_KIOSK
import com.xenia.ticket.utils.common.JwtUtils
import com.xenia.ticket.utils.common.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import retrofit2.HttpException
import java.util.Locale
import kotlin.getValue



class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val sessionManager: SessionManager by inject()
    private val loginRepository: LoginRepository by inject()
    private val companyRepository: CompanyRepository by inject()

    private var selectedLanguage: String? = ""
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupPasswordToggle()
        requestOverlayPermission()
        setAppLanguageAlwaysEnglish()

        if (sessionManager.isLoggedIn()) {

            val userType = UserType.fromValue(
                sessionManager.getUserType()
            )

            navigateAfterLogin(userType)
            return
        }

        val sharedPref = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
        val remembered = sharedPref.getBoolean("rememberMe", false)
        if (remembered) {
            binding.chkRememberMe?.let { checkBox ->
                checkBox.isChecked = true
            }
            binding.edtUserId.setText(sharedPref.getString("userId", ""))
            binding.edtPassword.setText(sharedPref.getString("password", ""))

        }

        binding.btnLogin.setOnClickListener {
            Log.d("LOGIN", "Login button clicked")

            val userId = binding.edtUserId.text.toString()
            val password = binding.edtPassword.text.toString()
            if (validateAndLogin(userId, password)) {
                Log.d("LOGIN", "Response: $userId,$password,$sharedPref")
                performLogin(userId, password, sharedPref)
            }
        }
    }
    private fun setAppLanguageAlwaysEnglish() {
        val locale = Locale.ENGLISH
        Locale.setDefault(locale)

        val config = resources.configuration
        config.setLocale(locale)

        resources.updateConfiguration(config, resources.displayMetrics)
    }

    private fun setupPasswordToggle() {
        val toggleIcon = binding.imgTogglePassword ?: return
        val passwordField = binding.edtPassword

        toggleIcon.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                passwordField.transformationMethod = null
                toggleIcon.setImageResource(R.drawable.eye_close)
            } else {
                passwordField.transformationMethod =
                    android.text.method.PasswordTransformationMethod.getInstance()
                toggleIcon.setImageResource(R.drawable.ic_eye)
            }
            passwordField.setSelection(passwordField.text?.length ?: 0)
        }
    }

private fun validateAndLogin(userId: String, password: String): Boolean {
    binding.apply {
        when {
            userId.isEmpty() -> {
                edtUserId.error = "User ID cannot be empty"
                edtUserId.requestFocus()
                return false
            }

            password.isEmpty() -> {
                edtPassword.error = "Password cannot be empty"
                edtPassword.requestFocus()
                return false
            }

            else -> {
                edtUserId.error = null
                edtPassword.error = null
            }
        }
    }
    return true
}
    private fun navigateAfterLogin(userType: UserType) {

        when (userType) {

            UserType.COUNTER_USER -> {
                if (!isSmallPortrait()) {
                    dismissLoader()
                    showSnackbar(
                        binding.root,
                        "COUNTER_USER has no permission for this device/orientation!"
                    )
                    return
                }

                startActivity(Intent(this, BillingTicketActivity::class.java))
                finish()
            }

            UserType.PROCESS_USER -> {
                dismissLoader()
                showSnackbar(
                    binding.root,
                    "PROCESS_USER is not allowed to login on this device!"
                )
            }

            UserType.CUSTOMER -> {
                if (!isXLargePortrait()) {
                    dismissLoader()
                    showSnackbar(
                        binding.root,
                        "User has no permission for this device/orientation!"
                    )
                    return
                }

                sessionManager.saveSelectedPrinter(PRINTER_KIOSK)
                startActivity(Intent(this, LanguageActivity::class.java))
                finish()
            }

            UserType.UNKNOWN -> {
                dismissLoader()
                showSnackbar(binding.root, "Unknown user type!")
            }
        }
    }
    private fun isSmallPortrait(): Boolean {
        val config = resources.configuration
        val screenSizeMask = config.screenLayout and Configuration.SCREENLAYOUT_SIZE_NORMAL
        val isPortrait = config.orientation == Configuration.ORIENTATION_PORTRAIT
        return screenSizeMask == Configuration.SCREENLAYOUT_SIZE_NORMAL && isPortrait
    }
    private fun isXLargePortrait(): Boolean {
        val config = resources.configuration
        val screenSizeMask = config.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
        val isPortrait = config.orientation == Configuration.ORIENTATION_PORTRAIT

        return screenSizeMask == Configuration.SCREENLAYOUT_SIZE_XLARGE && isPortrait
    }
    private fun performLogin(
        userId: String,
        password: String,
        sharedPref: android.content.SharedPreferences
    ) {

        if (!isInternetAvailable(applicationContext)) {
            hideKeyboard()
            dismissLoader()
            showSnackbar(binding.root, "Please connect to internet...")
            return
        }

        showLoader(this, "Logging...")


        CoroutineScope(Dispatchers.Main).launch {
            try {

                val response = loginRepository.login(userId, password)
                val token = response.Token

                sessionManager.clearSession()
                sessionManager.saveToken("Bearer $token")
                sessionManager.savePassword(password)

                sharedPref.edit {
                    if (binding.chkRememberMe?.isChecked == true) {
                        putBoolean("rememberMe", true)
                        putString("userId", userId)
                        putString("password", password)
                    } else {
                        clear()
                    }
                }

                val userType = UserType.fromValue(
                    JwtUtils.getUserType(token)
                )

                val company = companyRepository.getCompany()

                Log.d("LOGIN", "Company value = $company")

                startActivity(Intent(this@LoginActivity, SyncActivity::class.java))
                finish()

            } catch (e: HttpException) {

                val errorBody = e.response()?.errorBody()?.string()

                val msg = when (e.code()) {
                    404 -> "Incorrect Username!"
                    401 -> "Incorrect Password!"
                    else -> errorBody ?: e.message() ?: "Something went wrong!"
                }

                showSnackbar(binding.root, msg)

            } catch (e: Exception) {

                showSnackbar(
                    binding.root,
                    e.localizedMessage ?: "Something went wrong! Please try again."
                )

            } finally {
                dismissLoader()
            }
        }
    }


    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        val view = currentFocus ?: View(this)
        imm.hideSoftInputFromWindow(view.windowToken, 0)
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
    private val overlayPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {  }
}
