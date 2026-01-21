package com.example.ticket.ui.sreens.screen

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
import androidx.lifecycle.lifecycleScope
import com.example.ticket.R
import com.example.ticket.data.repository.LoginRepository
import com.example.ticket.databinding.ActivityLoginBinding
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

    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupPasswordToggle()
        requestOverlayPermission()


        if (sessionManager.isLoggedIn()) {

            val userType = UserType.fromValue(sessionManager.getUserType())

            lifecycleScope.launch {
                navigateAfterLogin(userType)
            }
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
    private fun navigateAfterLogin(userType: UserType) {

        val config = resources.configuration
        val screenSizeMask = config.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
        val isLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE
        val isPortrait = config.orientation == Configuration.ORIENTATION_PORTRAIT

        when (userType) {

            UserType.COUNTER_USER -> {

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
                    return
                }

                startActivity(Intent(this, LanguageActivity::class.java))
                finish()
            }

            UserType.PROCESS_USER -> {
                dismissLoader()
                showSnackbar(
                    binding.root,
                    "PROCESS_USER is not allowed to login on this device!"
                )
                return
            }

            UserType.CUSTOMER -> {
                if (screenSizeMask != Configuration.SCREENLAYOUT_SIZE_XLARGE || !isPortrait) {
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

                val config = resources.configuration
                val screenSizeMask = config.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
                val isLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE
                val isPortrait = config.orientation == Configuration.ORIENTATION_PORTRAIT

                val jwtPayload = JwtUtils.decodeJwt(token)
                val userTypeValue = jwtPayload.getString("UserType")
                val userType = UserType.fromValue(userTypeValue)

              
                when (userType) {

                    UserType.COUNTER_USER -> {

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

                        startActivity(
                            Intent(this@LoginActivity, LanguageActivity::class.java)
                        )
                        finish()
                    }

                    UserType.PROCESS_USER -> {

                        dismissLoader()
                        showSnackbar(
                            binding.root,
                            "PROCESS_USER is not allowed to login on this device!"
                        )
                        return@launch
                    }

                    else -> {
                        if (screenSizeMask != Configuration.SCREENLAYOUT_SIZE_XLARGE || !isPortrait) {
                            dismissLoader()
                            showSnackbar(
                                binding.root,
                                "User has no permission for this device/orientation!"
                            )
                            return@launch
                        }

                        sessionManager.saveSelectedPrinter(PRINTER_KIOSK)
                        startActivity(
                            Intent(this@LoginActivity, LanguageActivity::class.java)
                        )
                        finish()
                    }
                }

            } catch (e: HttpException) {

                val errorBody = e.response()?.errorBody()?.string()
                val msg = when (e.code()) {
                    404 -> "Incorrect Username!"
                    401 -> "Incorrect Password!"
                    else -> errorBody ?: e.message() ?: "Something went wrong!"
                }

                Log.e("LOGIN_ERROR", "Code: ${e.code()}, Body: $errorBody", e)
                showSnackbar(binding.root, msg)

            } catch (e: Exception) {

                Log.e("LOGIN_ERROR", "Unexpected error", e)
                showSnackbar(
                    binding.root,
                    e.localizedMessage ?: "Something went wrong! Please try again."
                )
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
