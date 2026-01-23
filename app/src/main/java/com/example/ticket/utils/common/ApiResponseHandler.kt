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

object ApiResponseHandler {

    fun <T> handleApiCall(
        activity: Activity,
        apiCall: suspend () -> T,
        onSuccess: (T) -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = apiCall()
                onSuccess(result)
            } catch (e: HttpException) {
                if (e.code() == 401) {
                    val errorBody = e.response()?.errorBody()?.string()
                    val message = try {
                        JSONObject(errorBody ?: "").optString("message")
                    } catch (ex: Exception) {
                        null
                    }

                    if (!message.isNullOrBlank()) {
                        // Show popup with server message
                        AlertDialog.Builder(activity)
                            .setTitle("Session Expired")
                            .setMessage(message)
                            .setCancelable(false)
                            .setPositiveButton("Logout") { _, _ ->
                                logoutUser(activity)
                            }
                            .show()
                    } else {
                        // Plain 401 → clear local DB silently
                        clearLocalData(activity)
                    }
                } else {
                    throw e
                }
            } catch (e: Exception) {
                throw e
            }
        }
    }

    private fun logoutUser(activity: Activity) {
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
