package com.xenia.ticket.ui.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xenia.ticket.R
import com.xenia.ticket.data.listeners.OnTicketClickListener
import com.xenia.ticket.data.repository.OrderRepository
import com.xenia.ticket.data.room.entity.Orders
import com.xenia.ticket.utils.common.Constants.LANGUAGE_ENGLISH
import com.xenia.ticket.utils.common.Constants.LANGUAGE_HINDI
import com.xenia.ticket.utils.common.Constants.LANGUAGE_KANNADA
import com.xenia.ticket.utils.common.Constants.LANGUAGE_MALAYALAM
import com.xenia.ticket.utils.common.Constants.LANGUAGE_MARATHI
import com.xenia.ticket.utils.common.Constants.LANGUAGE_PUNJABI
import com.xenia.ticket.utils.common.Constants.LANGUAGE_SINHALA
import com.xenia.ticket.utils.common.Constants.LANGUAGE_TAMIL
import com.xenia.ticket.utils.common.Constants.LANGUAGE_TELUGU
import com.xenia.ticket.utils.common.SessionManager
import com.google.android.material.button.MaterialButton
import com.xenia.ticket.data.network.model.ShowScheduleResponse
import com.xenia.ticket.data.repository.TicketRepository
import com.xenia.ticket.ui.adapter.ShowScheduleAdapter
import com.xenia.ticket.utils.common.CommonMethod.dismissLoader
import com.xenia.ticket.utils.common.CommonMethod.formatTime
import com.xenia.ticket.utils.common.CommonMethod.getTodayDay
import com.xenia.ticket.utils.common.CommonMethod.showLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import java.util.Locale
import kotlin.getValue

class CustomTicketPopupDialogue : DialogFragment() {
    private lateinit var txtTicketName: TextView
    private lateinit var txtComboTicketName: TextView
    private lateinit var txtTicketRate: TextView
    private lateinit var txtTicketChildRate: TextView
    private lateinit var txtQty: TextView
    private lateinit var txtTotalAmount: TextView
    private lateinit var editTextTickets: EditText
    private lateinit var editTextChildTickets: EditText
    private lateinit var backCallback: OnBackPressedCallback
    private lateinit var btnClear: ImageView
    private lateinit var icClose: RelativeLayout
    private lateinit var btnBack: RelativeLayout
    private lateinit var btnDone: MaterialButton
    private var firstClick: Boolean = true
    private val ticketRepository: OrderRepository by inject()
    private val activeTicketRepository: TicketRepository by inject()
    private val sessionManager: SessionManager by inject()
    private var ticketId: Int = 0
    private var ticketName: String = ""
    private var ticketNameMa: String = ""
    private var ticketNameTa: String = ""
    private var ticketNameKa: String = ""
    private var ticketNameTe: String = ""
    private var ticketNameHi: String = ""
    private var ticketNamePa: String = ""
    private var ticketNameMr: String = ""
    private var ticketNameSi: String = ""
    private var ticketRate: Double = 0.00
    private var ticketChildRate: Double = 0.00
    private var totalAmount: String = ""
    private var ticketCompanyId: Int = 0
    private var ticketCategoryId: Int = 0
    private var ticketCombo: Boolean = false
    private var ticketType: String = ""
    private var ticketChild: Boolean = false
    private var listener: OnTicketClickListener? = null
    var selectedSchedule: ShowScheduleResponse? = null
    private var activeEditText: EditText? = null
    private var comboShowId: Int? = null

    fun setListener(listener: OnTicketClickListener) {
        this.listener = listener
    }

    fun setData(
        ticketId: Int,
        ticketName: String,
        ticketNameMa: String,
        ticketNameTa: String,
        ticketNameKa: String,
        ticketNameTe: String,
        ticketNameHi: String,
        ticketNameSi: String,
        ticketNamePa: String,
        ticketNameMr: String,
        ticketCategoryId: Int,
        ticketCompanyId: Int,
        ticketRate: Double,
        ticketChildRate: Double,
        ticketCombo: Boolean,
        ticketType: String,
        ticketChild: Boolean
    ) {
        this.ticketId = ticketId
        this.ticketName = ticketName
        this.ticketNameMa = ticketNameMa
        this.ticketNameTa = ticketNameTa
        this.ticketNameKa = ticketNameKa
        this.ticketNameTe = ticketNameTe
        this.ticketNameHi = ticketNameHi
        this.ticketNameSi = ticketNameSi
        this.ticketNameMr = ticketNameMr
        this.ticketNamePa = ticketNamePa
        this.ticketRate = ticketRate
        this.ticketChildRate = ticketChildRate
        this.ticketCompanyId=ticketCompanyId
        this.ticketCategoryId=ticketCategoryId
        this.ticketCombo=ticketCombo
        this.ticketType = ticketType
        this.ticketChild = ticketChild
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(requireActivity(), theme).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            setCanceledOnTouchOutside(false)
            setCancelable(false)
        }
    }

    override fun onStart() {
        super.onStart()

        backCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                firstClick = true
                dismiss()
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            this,
            backCallback
        )
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.custom_ticket_dialogue, container, false)
    }

    @SuppressLint("SetTextI18n", "DefaultLocale", "ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        txtTicketName= view.findViewById(R.id.txtTicketName)
        txtComboTicketName= view.findViewById(R.id.txtComboTicketName)
        txtTicketRate = view.findViewById(R.id.txtTicketRate)
        txtTicketChildRate = view.findViewById(R.id.txtTicketChildRate)
        txtQty= view.findViewById(R.id.txtQty)
        txtTotalAmount = view.findViewById(R.id.totalAmount)
        editTextTickets = view.findViewById(R.id.editTextTickets)
        editTextChildTickets = view.findViewById(R.id.editTextChildTickets)
        editTextTickets.post { editTextTickets.selectAll() }
        val recyclerView = view.findViewById<RecyclerView>(R.id.showSchedule)
        applyTicketUIRules()
        val billingLang = sessionManager.getBillingSelectedLanguage()
        val appLang = sessionManager.getSelectedLanguage()

        val currentLang = billingLang.ifEmpty { appLang }

        btnBack = view.findViewById(R.id.btnBack)
        btnClear = view.findViewById(R.id.btnClear)
        icClose = view.findViewById(R.id.imgClose)
        btnDone = view.findViewById(R.id.btnDone)
        btnDone.text  = getString(R.string.done)

        val displayTicketName = when (currentLang) {
            LANGUAGE_ENGLISH -> ticketName
            LANGUAGE_MALAYALAM -> ticketNameMa
            LANGUAGE_TAMIL -> ticketNameTa
            LANGUAGE_KANNADA -> ticketNameKa
            LANGUAGE_TELUGU -> ticketNameTe
            LANGUAGE_HINDI -> ticketNameHi
            LANGUAGE_SINHALA -> ticketNameSi
            LANGUAGE_PUNJABI -> ticketNamePa
            LANGUAGE_MARATHI -> ticketNameMr
            else -> ticketName
        }
        txtTicketName.text = splitTextByWords(
            displayTicketName,
            maxCharsPerLine = 32,
            maxLines = 2
        )

        if(ticketCombo && ticketChild){
            val formattedAmount = String.format(Locale.ENGLISH, "%.2f", ticketRate)
            txtTicketRate.text = getString(R.string.no_of_tickets) +" "+ "Rs. $formattedAmount /-"

            val formattedChildAmount = String.format(Locale.ENGLISH, "%.2f", ticketRate)
            txtTicketChildRate.text = getString(R.string.no_of_child_tickets) +" "+ "Rs. $formattedChildAmount /-"
        }else{
            val formattedAmount = String.format(Locale.ENGLISH, "%.2f", ticketRate)
            txtTicketRate.text = getString(R.string.no_of_tickets) +" "+ "Rs. $formattedAmount /-"

            val formattedChildAmount = String.format(Locale.ENGLISH, "%.2f", ticketChildRate)
            txtTicketChildRate.text = getString(R.string.no_of_child_tickets) +" "+ "Rs. $formattedChildAmount /-"
        }



        if (ticketType.equals("SHOW", ignoreCase = true)) {

            recyclerView.visibility = View.VISIBLE
            txtComboTicketName.visibility = View.GONE

            val adapter = ShowScheduleAdapter(emptyList()) { selectedItem ->
                selectedSchedule = selectedItem
            }

            recyclerView.layoutManager = GridLayoutManager(requireContext(), 4)
            recyclerView.adapter = adapter

            lifecycleScope.launch {
                try {
                    showLoader(requireContext(), "Loading schedules...")
                    val day = getTodayDay()

                    val schedules = withContext(Dispatchers.IO) {
                        activeTicketRepository.getSchedules(ticketId, day)
                    }

                    val existingItem = withContext(Dispatchers.IO) {
                        ticketRepository.getCartItemByTicketId(ticketId)
                    }
                    dismissLoader()
                    if (schedules.isNotEmpty()) {
                        adapter.updateData(schedules)

                        selectedSchedule = when {
                            existingItem?.scheduleId != null && existingItem.scheduleId != 0 -> {
                                schedules.find { it.ScheduleId == existingItem.scheduleId }
                            }
                            else -> schedules[0]
                        }

                        selectedSchedule?.let {
                            adapter.setSelectedByScheduleId(it.ScheduleId)
                        }

                    } else {
                        recyclerView.visibility = View.GONE
                    }

                } catch (_: Exception) {
                    dismissLoader()
                    recyclerView.visibility = View.GONE
                }
            }
        }

        if (ticketCombo) {
            lifecycleScope.launch {
                try {
                    showLoader(requireContext(), "Loading schedules...")

                    val result = withContext(Dispatchers.IO) {
                        activeTicketRepository.getComboResult(ticketId)
                    }

                    comboShowId = result.showId

                    txtComboTicketName.visibility = View.VISIBLE
                    txtComboTicketName.text = result.names.joinToString(" | ")

                    if (result.showId != null) {

                        recyclerView.visibility = View.VISIBLE

                        val adapter = ShowScheduleAdapter(emptyList()) { selectedItem ->
                            selectedSchedule = selectedItem
                        }

                        recyclerView.layoutManager = GridLayoutManager(requireContext(), 4)
                        recyclerView.adapter = adapter

                        val day = getTodayDay()

                        val schedules = withContext(Dispatchers.IO) {
                            activeTicketRepository.getSchedules(result.showId, day)
                        }

                        val existingItem = withContext(Dispatchers.IO) {
                            ticketRepository.getCartItemByTicketId(ticketId)
                        }

                        if (schedules.isNotEmpty()) {
                            adapter.updateData(schedules)

                            selectedSchedule = when {
                                existingItem?.scheduleId != null && existingItem.scheduleId != 0 -> {
                                    schedules.find { it.ScheduleId == existingItem.scheduleId }
                                }
                                else -> schedules[0]
                            }

                            selectedSchedule?.let {
                                adapter.setSelectedByScheduleId(it.ScheduleId)
                            }

                        } else {
                            recyclerView.visibility = View.GONE
                        }

                    } else {
                        recyclerView.visibility = View.GONE
                    }

                } catch (_: Exception) {
                    recyclerView.visibility = View.GONE
                } finally {
                    dismissLoader()
                }
            }
        }
        editTextTickets.inputType = 0
        editTextChildTickets.inputType = 0

        activeEditText = editTextTickets
        editTextTickets.requestFocus()
        updateFocusUI(editTextTickets)

        lifecycleScope.launch {
            val cartItem = ticketRepository.getCartItemByTicketId(ticketId)

            withContext(Dispatchers.Main) {

                val childOnlyMode = isChildOnlyMode()

                if (cartItem != null) {
                    editTextTickets.setText(cartItem.ticketQty.toString())
                    editTextChildTickets.setText(cartItem.ticketChildQty.toString())
                } else {
                    if (childOnlyMode) {
                        editTextTickets.setText("0")
                        editTextChildTickets.setText("1")
                    } else {
                        editTextTickets.setText("1")
                        editTextChildTickets.setText("0")
                    }
                }

                activeEditText = if (childOnlyMode) editTextChildTickets else editTextTickets
                activeEditText?.requestFocus()
                updateFocusUI(activeEditText)
                updateAmounts()
            }
        }
        hideSoftKeyboard()

        val numberButtons = listOf(
            R.id.btnOne, R.id.btnTwo, R.id.btnThree,
            R.id.btnFour, R.id.btnFive, R.id.btnSix,
            R.id.btnSeven, R.id.btnEight, R.id.btnNine,
            R.id.btnZero
        )

        numberButtons.forEach { buttonId ->
            view.findViewById<TextView>(buttonId).setOnClickListener { v ->
                if(firstClick){
                    firstClick = false
                    activeEditText?.setText("")
                }
                appendToFocusedEditText((v as TextView).text.toString())
            }
        }


        btnBack.setOnClickListener {
            removeLastCharacterFromFocusedEditText()
        }

        icClose.setOnClickListener {
            editTextTickets.setText("1")
            firstClick = true
            dismiss()
        }

        btnDone.setOnClickListener {
            val childOnlyMode =
                ticketType.equals("TICKET", true) && ticketCombo && ticketChild

            val quantityInput = editTextTickets.text.toString().toIntOrNull() ?: 0
            val childQuantityInput = editTextChildTickets.text.toString().toIntOrNull() ?: 0

            val quantity = if (childOnlyMode) 0 else quantityInput

            val totalQty = if (childOnlyMode) childQuantityInput else (quantity + childQuantityInput)

            if (childOnlyMode) {
                if (childQuantityInput <= 0) {
                    Toast.makeText(requireContext(), "Please enter child quantity", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            } else {
                if (quantity <= 0) {
                    Toast.makeText(requireContext(), "Please enter a valid quantity", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            val isSeatCheckRequired =
                ticketCombo || ticketType.equals("SHOW", ignoreCase = true)

            if (isSeatCheckRequired && selectedSchedule == null) {
                Toast.makeText(requireContext(), "Please select a schedule", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isSeatCheckRequired) {
                val availableSeats = selectedSchedule?.AvailableSeats ?: 0

                if (totalQty > availableSeats) {
                    Toast.makeText(
                        requireContext(),
                        "Only $availableSeats seats available",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
            }

            val finalChildRate = if (childOnlyMode) ticketRate else ticketChildRate

            val adultTotal = ticketRate * quantity
            val childTotal = finalChildRate * childQuantityInput
            val grandTotal = adultTotal + childTotal

            val cartItem = Orders(
                ticketId = ticketId,
                ticketName = ticketName,
                ticketNameMa = ticketNameMa,
                ticketNameTa = ticketNameTa,
                ticketNameTe = ticketNameTe,
                ticketNameKa = ticketNameKa,
                ticketNameHi = ticketNameHi,
                ticketNamePa = ticketNamePa,
                ticketNameMr = ticketNameMr,
                ticketNameSi = ticketNameSi,
                ticketCategoryId = ticketCategoryId,
                ticketCompanyId = ticketCompanyId,
                ticketCreatedDate = System.currentTimeMillis().toString(),
                ticketCreatedBy = 0,
                ticketActive = true,
                ticketCombo = ticketCombo,
                ticketType = ticketType,
                daName = "",
                ticketRate = ticketRate,
                ticketChildRate = finalChildRate,
                ticketQty = quantity,
                ticketChildQty = childQuantityInput,
                ticketTotalAmount = grandTotal,
                daPhoneNumber = "",
                daProofId = "",
                daProof = "",
                daImg = byteArrayOf(),
                daCustRefNo = "",
                daNpciTransId = "",
                screenId = selectedSchedule?.ScreenId ?: 0,
                scheduleId = selectedSchedule?.ScheduleId ?: 0,
                scheduleDay = selectedSchedule?.ShowDay ?: "",
                scheduleTime = selectedSchedule?.let {
                    "${formatTime(it.StartTime)} - ${formatTime(it.EndTime)}"
                } ?: "",
                screenName = selectedSchedule?.ScreenName ?: "",
                ticketChild = ticketChild
            )

            lifecycleScope.launch {
                ticketRepository.insertCartItem(cartItem)
                listener?.onTicketAdded(ticketId)
                firstClick = true
                dismiss()
            }
        }

        btnClear.setOnClickListener {
            clearFocusedEditText()
        }

        editTextTickets.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                activeEditText = editTextTickets
                updateFocusUI(editTextTickets)
                editTextTickets.requestFocus()
                hideSoftKeyboard()
            }
            true
        }

        editTextChildTickets.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                activeEditText = editTextChildTickets
                updateFocusUI(editTextChildTickets)
                editTextChildTickets.requestFocus()
                hideSoftKeyboard()
            }
            true
        }
    }

    private fun applyTicketUIRules() {

        val childOnlyMode = isChildOnlyMode()

        val showAdult = !childOnlyMode
        val showChild = childOnlyMode || ticketChildRate > 0.0

        editTextTickets.visibility = if (showAdult) View.VISIBLE else View.GONE
        txtTicketRate.visibility = if (showAdult) View.VISIBLE else View.GONE

        editTextChildTickets.visibility = if (showChild) View.VISIBLE else View.GONE
        txtTicketChildRate.visibility = if (showChild) View.VISIBLE else View.GONE

        if (childOnlyMode) {
            editTextTickets.setText("0")
            editTextChildTickets.setText("1")

            activeEditText = editTextChildTickets
        } else {
            if (editTextTickets.text.isNullOrEmpty()) {
                editTextTickets.setText("1")
            }
            if (editTextChildTickets.text.isNullOrEmpty()) {
                editTextChildTickets.setText("0")
            }

            activeEditText = editTextTickets
        }

        activeEditText?.requestFocus()
        updateFocusUI(activeEditText)

        updateAmounts()
    }

    fun splitTextByWords(
        text: String,
        maxCharsPerLine: Int,
        maxLines: Int
    ): String {

        val words = text.trim().split("\\s+".toRegex())
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {
            if (("$currentLine $word").trim().length <= maxCharsPerLine) {
                currentLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            } else {
                lines.add(currentLine)
                currentLine = word
                if (lines.size == maxLines - 1) break
            }
        }

        if (currentLine.isNotEmpty() && lines.size < maxLines) {
            lines.add(currentLine)
        }

        return lines.joinToString("\n")
    }

    private fun appendToFocusedEditText(text: String) {
        val editText = activeEditText ?: return

        if (firstClick) {
            firstClick = false
            editText.setText(text)
        } else {
            val currentText = editText.text.toString()
            if (currentText == "0") {
                editText.setText(text)
            } else {
                val newText = currentText + text
                editText.setText(newText)
            }
        }

        editText.setSelection(editText.text.length)
        updateAmounts()
    }


    private fun removeLastCharacterFromFocusedEditText() {
        val editText = activeEditText ?: return

        val currentText = editText.text?.toString().orEmpty()

        if (currentText.isNotEmpty()) {
            val updatedText = currentText.dropLast(1)
            editText.setText(updatedText)
            editText.setSelection(updatedText.length)
        }

        updateAmounts()
    }

    private fun clearFocusedEditText() {
        val editText = activeEditText ?: return

        editText.text?.clear()
        editText.requestFocus()

        updateAmounts()
    }


    private fun hideSoftKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(editTextTickets.windowToken, 0)
    }

    private fun isChildOnlyMode(): Boolean {
        return ticketType.equals("TICKET", true) && ticketCombo && ticketChild
    }

    @SuppressLint("DefaultLocale", "SetTextI18n")
    private fun updateAmounts() {

        val childOnlyMode = isChildOnlyMode()

        val adultQtyInput = editTextTickets.text.toString().toIntOrNull() ?: 0
        val childQtyInput = editTextChildTickets.text.toString().toIntOrNull() ?: 0


        val adultQty = if (childOnlyMode) 0 else adultQtyInput

        val totalQty = if (childOnlyMode) childQtyInput else adultQty + childQtyInput

        val isSeatCheckRequired =
            ticketCombo || ticketType.equals("SHOW", true)

        if (isSeatCheckRequired && selectedSchedule != null) {

            val availableSeats = selectedSchedule?.AvailableSeats ?: Int.MAX_VALUE

            if (totalQty > availableSeats) {

                Toast.makeText(
                    requireContext(),
                    "Maximum $availableSeats seats allowed",
                    Toast.LENGTH_SHORT
                ).show()

                if (childOnlyMode) {
                    editTextChildTickets.setText(availableSeats.toString())
                } else {
                    val allowedAdult = minOf(adultQty, availableSeats)
                    val allowedChild = minOf(childQtyInput, availableSeats - allowedAdult)

                    editTextTickets.setText(allowedAdult.toString())
                    editTextChildTickets.setText(allowedChild.toString())
                }

                return
            }
        }

        // =========================
        // RATE LOGIC
        // =========================
        val finalChildRate = if (childOnlyMode) ticketRate else ticketChildRate

        val adultTotal = ticketRate * adultQty
        val childTotal = finalChildRate * childQtyInput

        val grandTotal = adultTotal + childTotal
        totalAmount = grandTotal.toString()

        // =========================
        // UI DISPLAY
        // =========================
        if (adultQty == 0 && childQtyInput == 0) {
            txtQty.text = getString(R.string.txt_amount) + " :"
            txtTotalAmount.visibility = View.GONE
            return
        }

        txtTotalAmount.visibility = View.VISIBLE

        val adultText = if (adultQty > 0) {
            "${getString(R.string.no_of_tickets)} : $adultQty x ${"%.2f".format(ticketRate)}"
        } else ""

        val childText = if (childQtyInput > 0) {
            "${getString(R.string.no_of_child_tickets)} : $childQtyInput x ${"%.2f".format(finalChildRate)}"
        } else ""

        txtQty.text = when {
            adultQty > 0 && childQtyInput > 0 -> "$adultText\n$childText"
            adultQty > 0 -> adultText
            else -> childText
        }

        txtTotalAmount.text = "Rs. %.2f/-".format(grandTotal)
    }

    private fun updateFocusUI(focused: EditText?) {
        val normal = R.drawable.edittext_bg
        val selected = R.drawable.edittext_selected

        editTextTickets.setBackgroundResource(
            if (focused == editTextTickets) selected else normal
        )

        editTextChildTickets.setBackgroundResource(
            if (focused == editTextChildTickets) selected else normal
        )
    }
}