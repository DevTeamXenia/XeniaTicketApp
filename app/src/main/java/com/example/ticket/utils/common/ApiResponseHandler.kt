package com.example.ticket.utils.common



import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import com.example.ticket.data.repository.CategoryRepository
import com.example.ticket.data.repository.TicketRepository
import com.example.ticket.ui.sreens.screen.LoginActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException
import org.koin.android.ext.android.inject
import android.widget.Toast

object ApiResponseHandler {

    fun <T> handleApiCall(
        activity: Activity,
        apiCall: suspend () -> T,
        onSuccess: (T) -> Unit
    ) {
        android.util.Log.d("ApiResponseHandler", "handleApiCall started")
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = apiCall()
                android.util.Log.d("ApiResponseHandler", "apiCall succeeded, calling onSuccess")
                onSuccess(result)
            } catch (e: HttpException) {
                android.util.Log.e("ApiResponseHandler", "HttpException caught: ${e.code()}", e)
                if (e.code() == 401) {
                    val errorBody = e.response()?.errorBody()?.string()
                    val message = try {
                        JSONObject(errorBody ?: "").optString("message")
                    } catch (ex: Exception) {
                        null
                    }

                    if (!message.isNullOrBlank()) {
                            android.util.Log.d("ApiResponseHandler", "Showing dialog for other 401")
                            AlertDialog.Builder(activity)
                                .setTitle("Logout !!")
                                .setMessage(message)
                                .setCancelable(false)
                                .setPositiveButton("Logout") { _, _ ->
                                    logoutUser(activity)
                                }
                                .show()

                        } else if (message == "You have been logged out because your account was used on another device.") {
                        android.util.Log.d("ApiResponseHandler", "Showing Toast for multi-device logout")
                        Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
                        clearLocalData(activity)
                        logoutUser(activity)
                    } else {
                        android.util.Log.d("ApiResponseHandler", "Silent logout for plain 401")
                        clearLocalData(activity)
                        logoutUser(activity)
                    }
                } else {
                    android.util.Log.e("ApiResponseHandler", "Non-401 HttpException: ${e.code()} - ${e.message()}", e)
                    throw e
                }
            } catch (e: Exception) {
                android.util.Log.e("ApiResponseHandler", "Other exception in handleApiCall", e)
                throw e
            }
        }
    }

    fun logoutUser(activity: Activity) {
        val sessionManager: SessionManager by activity.inject()
        sessionManager.clearSession()

        activity.startActivity(
            Intent(activity, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        activity.finish()
    }

    private fun clearLocalData(activity: Activity) {
        val ticketRepository: TicketRepository by activity.inject()
        val categoryRepository: CategoryRepository by activity.inject()

        CoroutineScope(Dispatchers.IO).launch {
            ticketRepository.clearAllData()
            categoryRepository.clearAllData()
        }
    }
}
