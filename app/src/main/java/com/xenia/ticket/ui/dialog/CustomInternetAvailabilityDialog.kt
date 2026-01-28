package com.xenia.ticket.ui.dialog



import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import com.xenia.ticket.R
import androidx.fragment.app.DialogFragment
import com.xenia.ticket.utils.common.CommonMethod

class CustomInternetAvailabilityDialog : DialogFragment() {

    interface InternetAvailabilityListener {
        fun onRetryClicked()
        fun onDialogInactive()
    }
    private var listener: InternetAvailabilityListener? = null
    private val inactivityHandler = Handler(Looper.getMainLooper())
    private val inactivityRunnable = Runnable {
        dismiss()
        listener?.onDialogInactive()
    }
    private val inactivityTimeout = 30000L

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is InternetAvailabilityListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement InternetAvailabilityListener")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCanceledOnTouchOutside(false)
        dialog.setCancelable(false)
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_no_internet, container, false)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.btnRetry).setOnClickListener {
            resetInactivityTimer()
            if (CommonMethod.isInternetAvailable(requireContext())) {
                listener?.onRetryClicked()
                dismiss()
            }
        }

        view.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                resetInactivityTimer()
            }
            false
        }

        startInactivityTimer()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.55).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun startInactivityTimer() {
        inactivityHandler.postDelayed(inactivityRunnable, inactivityTimeout)
    }

    private fun resetInactivityTimer() {
        inactivityHandler.removeCallbacks(inactivityRunnable)
        inactivityHandler.postDelayed(inactivityRunnable, inactivityTimeout)
    }

    override fun onDestroy() {
        super.onDestroy()
        inactivityHandler.removeCallbacks(inactivityRunnable)
    }
}