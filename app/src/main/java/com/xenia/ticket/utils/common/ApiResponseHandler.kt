package com.xenia.ticket.utils.common

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import com.xenia.ticket.data.repository.CategoryRepository
import com.xenia.ticket.data.repository.TicketRepository
import com.xenia.ticket.ui.screens.kiosk.LoginActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.HttpException
import org.koin.android.ext.android.inject
import android.widget.Toast

object ApiResponseHandler {

    suspend fun <T> handleApiCall(
        activity: Activity,
        apiCall: suspend () -> T
    ): T? {
        return try {
            android.util.Log.d("ApiResponseHandler", "API CALL START")

            val result = withContext(Dispatchers.IO) {
                apiCall()
            }

            android.util.Log.d("ApiResponseHandler", "API CALL SUCCESS")
            result

        } catch (e: HttpException) {

            android.util.Log.e("ApiResponseHandler", "HTTP ERROR: ${e.code()}", e)

            if (e.code() == 401) {

                val errorBody = e.response()?.errorBody()?.string()
                val message = try {
                    JSONObject(errorBody ?: "").optString("message")
                } catch (_: Exception) {
                    null
                }

                withContext(Dispatchers.Main) {

                    if (!message.isNullOrBlank()) {
                        AlertDialog.Builder(activity)
                            .setTitle("Logout !!")
                            .setMessage(message)
                            .setCancelable(false)
                            .setPositiveButton("Logout") { _, _ ->
                                logoutUser(activity)
                            }
                            .show()
                    } else {
                        Toast.makeText(activity, "Session expired", Toast.LENGTH_LONG).show()
                        clearLocalData(activity)
                        logoutUser(activity)
                    }
                }
            }

            null

        } catch (e: Exception) {
            android.util.Log.e("ApiResponseHandler", "GENERAL ERROR", e)
            null
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

    private suspend fun clearLocalData(activity: Activity) {
        val ticketRepository: TicketRepository by activity.inject()
        val categoryRepository: CategoryRepository by activity.inject()

        ticketRepository.clearAllData()
        categoryRepository.clearAllData()
    }
}