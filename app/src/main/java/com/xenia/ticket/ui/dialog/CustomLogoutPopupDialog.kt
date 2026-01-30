package com.xenia.ticket.ui.dialog

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.TextView
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.xenia.ticket.R
import com.xenia.ticket.data.repository.TicketRepository
import com.xenia.ticket.ui.sreens.kiosk.LoginActivity
import com.xenia.ticket.utils.common.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject


class CustomLogoutPopupDialog : DialogFragment() {

    private val sessionManager: SessionManager by inject()
    private val ticketRepository: TicketRepository by inject()

    private lateinit var tvMessage: TextView
    private lateinit var btnCancel: Button
    private lateinit var btnYes: Button

    private var isForcedLogout = false

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        dialog.setCancelable(false)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        inflater.inflate(R.layout.custom_warning_popup_dialog, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvMessage = view.findViewById(R.id.tvMessage)
        btnCancel = view.findViewById(R.id.btnCancel)
        btnYes = view.findViewById(R.id.btnYes)

        tvMessage.text = getString(R.string.do_you_want_to_logout)

        btnCancel.setOnClickListener { dismiss() }

        btnYes.setOnClickListener {
            if (isForcedLogout) {
                performLocalLogout()
            } else {
                val token = sessionManager.getToken()
                if (token.isNullOrEmpty()) {
                    performLocalLogout()
                } else {
                    callLogoutApi()
                }
            }
        }

    }
    private fun callLogoutApi() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ticketRepository.logout(sessionManager.getToken().toString())
                }

                if (
                    response.status == "Error" &&
                    response.message.contains("another device", true)
                ) {
                    showForcedLogoutState(response.message)
                } else {
                    performLocalLogout()
                }

            } catch (e: Exception) {
                performLocalLogout()
            }
        }
    }
    private fun showForcedLogoutState(message: String) {
        isForcedLogout = true
        tvMessage.text = message
        btnCancel.visibility = View.GONE
        btnYes.text = getString(R.string.logout)
    }

    private fun performLocalLogout() {
        if (!isAdded) return
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                sessionManager.clearSession()
                ticketRepository.clearAllData()
            }

            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            activity?.finish()
        }
    }
}
