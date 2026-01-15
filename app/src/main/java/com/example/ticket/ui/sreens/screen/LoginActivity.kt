package com.example.ticket.ui.sreens.screen

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.example.ticket.R
import com.example.ticket.data.enum.UserType
import com.example.ticket.data.repository.CompanyRepository
import com.example.ticket.data.repository.LoginRepository
import com.example.ticket.databinding.ActivityLoginBinding
import com.example.ticket.ui.sreens.billing.Billing_selection_Activity
import com.example.ticket.utils.common.CommonMethod.dismissLoader
import com.example.ticket.utils.common.CommonMethod.isInternetAvailable
import com.example.ticket.utils.common.CommonMethod.showSnackbar
import com.example.ticket.utils.common.Constants.PRINTER_KIOSK
import com.example.ticket.utils.common.JwtUtils
import com.example.ticket.utils.common.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import retrofit2.HttpException
import kotlin.getValue


class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val sessionManager: SessionManager by inject()
    private val loginRepository: LoginRepository by inject()
    private val companyRepository: CompanyRepository by inject()
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupPasswordToggle()
        requestOverlayPermission()
        if (sessionManager.isLoggedIn()) {

            val userTypeString = sessionManager.getUserType()
            val selectedLanguage = sessionManager.getSelectedLanguage()

            lifecycleScope.launch {


                val userType = UserType.fromValue(userTypeString)

                if (userType == UserType.COUNTER_USER) {
                    if (selectedLanguage.isNotEmpty()) {
                        startActivity(Intent(applicationContext, Billing_selection_Activity::class.java))
                    } else {
                        startActivity(Intent(applicationContext, LanguageActivity::class.java))
                    }
                }
                finish()
            }
            return
        }
        val sharedPref = getSharedPreferences("LoginPrefs", MODE_PRIVATE)

        binding.btnLogin.setOnClickListener {
            val userId = binding.edtUserId.text.toString()
            val password = binding.edtPassword.text.toString()
            if (validateAndLogin(userId, password)) {
                performLogin(userId, password, sharedPref)
            }
        }
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

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = loginRepository.login(userId, password)
                val token = response.Token
                sessionManager.clearSession()
                sessionManager.saveToken( "Bearer $token")
                sessionManager.savePassword(password)


                val config = resources.configuration
                val screenSizeMask = config.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
                val isLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE
                val isPortrait = config.orientation == Configuration.ORIENTATION_PORTRAIT
                val jwtPayload = JwtUtils.decodeJwt(token)
                val userTypeValue = jwtPayload.getString("UserType")
                val userType = UserType.fromValue(userTypeValue)

                when (userType) {

                    UserType.COUNTER_USER -> {
                        Log.d("LOGIN_DEBUG", "Entering COUNTER_USER block")

                        val allowedForLarge =
                            screenSizeMask == Configuration.SCREENLAYOUT_SIZE_LARGE
                        val allowedForXLarge =
                            screenSizeMask == Configuration.SCREENLAYOUT_SIZE_XLARGE && isLandscape

                        if (!allowedForLarge && !allowedForXLarge) {
                            dismissLoader()
                            showSnackbar(
                                binding.root,
                                "COUNTER_USER has no permission for this device/orientation!"
                            )
                            return@launch
                        }
                        startActivity(Intent(this@LoginActivity, Billing_selection_Activity::class.java))
                        finish()
                    }

                    else -> {
                        Log.d("LOGIN_DEBUG", "Entering NORMAL_USER block")

                        if (screenSizeMask != Configuration.SCREENLAYOUT_SIZE_XLARGE || !isPortrait) {
                            dismissLoader()
                            showSnackbar(
                                binding.root,
                                "NORMAL_USER has no permission for this device/orientation!"
                            )
                            return@launch
                        }

                        sessionManager.saveSelectedPrinter(PRINTER_KIOSK)
                        startActivity(Intent(this@LoginActivity, LanguageActivity::class.java))
                        finish()
                    }
                }

            } catch (e: HttpException) {
                val msg = when (e.code()) {
                    404 -> "Incorrect Username!"
                    401 -> "Incorrect Password!"
                    else -> "Something went wrong!"
                }
                showSnackbar(binding.root, msg)
            } catch (_: Exception) {
                showSnackbar(binding.root, "Something went wrong! Please try again.")
            } finally {
                hideKeyboard()
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
