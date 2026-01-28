package com.xenia.ticket.ui.dialog

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.xenia.ticket.R
import com.xenia.ticket.data.repository.CompanyRepository
import com.xenia.ticket.ui.sreens.screen.LanguageActivity
import com.xenia.ticket.utils.common.CommonMethod.setLocale
import com.xenia.ticket.utils.common.Constants.LANGUAGE_ENGLISH
import com.xenia.ticket.utils.common.SessionManager
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject


class CustomInactivityDialog(private val callback: InactivityCallback) : DialogFragment() {
    private val sessionManager: SessionManager by inject()
    private val companyRepository: CompanyRepository by inject()
    interface InactivityCallback {
        fun resetInactivityTimer()
    }

    private var countdownTimer: CountDownTimer? = null
    private var endTime: Long = 0L
    private lateinit var btnNo: Button
    private var hasRedirected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(requireActivity(), theme).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setCanceledOnTouchOutside(false)
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_inactivity, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnNo = view.findViewById(R.id.btnNo)

        startCountdown()

        view.findViewById<Button>(R.id.btnNo).setOnClickListener {
            redirectToLanguageActivity()
        }

        view.findViewById<Button>(R.id.btnYes).setOnClickListener {
            callback.resetInactivityTimer()
            countdownTimer?.cancel()
            dismiss()
        }
    }

    private fun startCountdown() {
        val countdownTime = 10_000L
        endTime = System.currentTimeMillis() + countdownTime

        countdownTimer = object : CountDownTimer(countdownTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if (!isAdded || hasRedirected) return
                val seconds = ((endTime - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
                btnNo.text = getString(R.string.no) + " ($seconds)"
            }

            override fun onFinish() {
                if (!isAdded || hasRedirected) return
                btnNo.text = getString(R.string.no) + " (0)"
                redirectToLanguageActivity()
            }
        }.start()
    }

    private fun redirectToLanguageActivity() {
        if (!isAdded || hasRedirected) return
        hasRedirected = true

        countdownTimer?.cancel()

        viewLifecycleOwner.lifecycleScope.launch {
            sessionManager.saveSelectedLanguage(LANGUAGE_ENGLISH)
            setLocale(requireContext(), LANGUAGE_ENGLISH)

            val intent = Intent(requireContext(), LanguageActivity::class.java)
            startActivity(intent)

            dismissAllowingStateLoss()
        }
    }


    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.55).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countdownTimer?.cancel()
    }
}
