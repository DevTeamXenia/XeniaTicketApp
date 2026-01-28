package com.xenia.ticket.utils.common

import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.xenia.ticket.ui.dialog.CustomInactivityDialog
import kotlin.collections.any
import kotlin.let


class InactivityHandler(
    private val activity: AppCompatActivity,
    private val fragmentManager: FragmentManager,
    private val inactivityDialog: CustomInactivityDialog
) {
    private val inactivityRunnable = Runnable {
        if (!activity.isFinishing && !activity.isDestroyed) {
            showDialogSafely()
        }
    }

    private val inactivityTimeout = 30000L
    private val timer: Handler = Handler(Looper.getMainLooper())
    private var isInactivityCheckPaused = false

    init {
        resetTimer()
    }

    fun resetTimer() {
        if (!isInactivityCheckPaused) {
            timer.removeCallbacks(inactivityRunnable)
            timer.postDelayed(inactivityRunnable, inactivityTimeout)
        }
    }

    fun pauseInactivityCheck() {
        isInactivityCheckPaused = true
        timer.removeCallbacks(inactivityRunnable)
    }

    fun resumeInactivityCheck() {
        isInactivityCheckPaused = false
        resetTimer()
    }

    fun cleanup() {
        timer.removeCallbacks(inactivityRunnable)
    }

    fun showDialogSafely() {
        val hasDialogShowing = fragmentManager.fragments.any {
            it is DialogFragment && it.dialog?.isShowing == true
        }
        if (hasDialogShowing) return

        val ft = fragmentManager.beginTransaction()
        fragmentManager.findFragmentByTag("inactivity_dialog")?.let { ft.remove(it) }
        ft.add(inactivityDialog, "inactivity_dialog")
        ft.commitAllowingStateLoss()
    }

}
