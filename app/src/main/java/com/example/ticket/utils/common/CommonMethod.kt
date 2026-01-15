package com.example.ticket.utils.common

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.ticket.R
import com.google.android.material.snackbar.Snackbar
import java.security.SecureRandom

object CommonMethod {
    private var loader: AlertDialog? = null

    fun showLoader(context: Context, message: String) {
        if (context is AppCompatActivity && context.isFinishing) {
            return
        }

        val builder = AlertDialog.Builder(context, R.style.TransparentAlertDialog)
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.custom_loader, null)

        view.findViewById<TextView>(R.id.loaderMessage).text = message

        builder.setView(view)
        builder.setCancelable(false)

        com.example.ticket.utils.common.CommonMethod.loader = builder.create()
        com.example.ticket.utils.common.CommonMethod.loader?.show()
    }
    fun dismissLoader() {
        loader?.dismiss()
        loader = null
    }
    fun showSnackbar(view: View, message: String) {
        Snackbar.make(view, message, Snackbar.LENGTH_LONG).show()
    }

    fun generateNumericTransactionReferenceID(): String {
        val secureRandom = SecureRandom()
        val numberStringBuilder = StringBuilder(10)

        numberStringBuilder.append(secureRandom.nextInt(9) + 1)

        for (i in 1 until 10) {
            numberStringBuilder.append(secureRandom.nextInt(10))
        }

        return numberStringBuilder.toString()
    }


    @SuppressLint("ObsoleteSdkInt")
    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else {
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo?.isConnected ?: false
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun View.enableInactivityReset(inactivityHandler: InactivityHandler) {

        this.setOnKeyListener { _, _, _ ->
            inactivityHandler.resetTimer()
            false
        }

        this.setOnTouchListener { _, _ ->
            inactivityHandler.resetTimer()
            false
        }
    }


    fun getScreenSize(context: Context): String {
        val config = context.resources.configuration
        val screenSizeMask = config.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
        return when (screenSizeMask) {
            Configuration.SCREENLAYOUT_SIZE_SMALL -> "Small"
            Configuration.SCREENLAYOUT_SIZE_NORMAL -> "Normal"
            Configuration.SCREENLAYOUT_SIZE_LARGE -> "Large"
            Configuration.SCREENLAYOUT_SIZE_XLARGE -> "XLarge"
            else -> "Undefined"
        }
    }
    fun isLandscapeScreen(context: Context): Boolean {
        val orientation = context.resources.configuration.orientation
        return orientation == Configuration.ORIENTATION_LANDSCAPE
    }




}