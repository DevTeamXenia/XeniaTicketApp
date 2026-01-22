package com.example.ticket.ui.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.ticket.R
import com.example.ticket.data.listeners.OnTicketClickListener
import com.example.ticket.data.repository.TicketRepository
import com.example.ticket.data.room.entity.Ticket
import com.example.ticket.utils.common.Constants.LANGUAGE_ENGLISH
import com.example.ticket.utils.common.Constants.LANGUAGE_HINDI
import com.example.ticket.utils.common.Constants.LANGUAGE_KANNADA
import com.example.ticket.utils.common.Constants.LANGUAGE_MALAYALAM
import com.example.ticket.utils.common.Constants.LANGUAGE_MARATHI
import com.example.ticket.utils.common.Constants.LANGUAGE_PUNJABI
import com.example.ticket.utils.common.Constants.LANGUAGE_SINHALA
import com.example.ticket.utils.common.Constants.LANGUAGE_TAMIL
import com.example.ticket.utils.common.Constants.LANGUAGE_TELUGU
import com.example.ticket.utils.common.InactivityHandler
import com.example.ticket.utils.common.SessionManager
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import java.util.Locale
import kotlin.getValue

class CustomTicketPopupDialogue : DialogFragment() {
    private lateinit var txtTicketName: TextView
    private lateinit var txtTicketRate: TextView
    private lateinit var txtTickets: TextView
    private lateinit var txtQty: TextView
    private lateinit var txtTotalAmount: TextView
    private lateinit var editTextTickets: EditText
    private lateinit var inactivityHandler: InactivityHandler
    private lateinit var btnClear: ImageView
    private lateinit var icClose: ImageView
    private lateinit var btnBack: RelativeLayout

    private lateinit var btnDone: MaterialButton

    private var firstClick: Boolean = true

    private val ticketRepository: TicketRepository by inject()
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
    private var totalAmount: String = ""
    private var ticketCompanyId: Int = 0
    private var ticketCategoryId: Int = 0
    private var listener: OnTicketClickListener? = null

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
        ticketCtegoryId: Int,
        ticketCompanyId: Int,
        ticketRate: Double
    ) {
        this.ticketId = ticketId
        this.ticketName = ticketName
        this.ticketNameMa = ticketNameMa
        this.ticketNameTa = ticketNameTa
        this.ticketNameKa = ticketNameKa
        this.ticketNameTe = ticketNameTe
        this.ticketNameHi = ticketNameHi
        this.ticketNameSi = ticketNameSi
        this.ticketNameMr=ticketNameMr
        this.ticketNamePa=ticketNamePa
        this.ticketRate = ticketRate
        this.ticketCompanyId=ticketCompanyId
        this.ticketCategoryId=ticketCtegoryId


    }



    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return object : Dialog(requireActivity(), theme) {
            @Deprecated("Deprecated in Java", ReplaceWith("dismiss()"))
            override fun onBackPressed() {
                firstClick = true
                dismiss()
            }
        }.apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            setCanceledOnTouchOutside(false)
            setCancelable(false)
        }

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
        txtTickets = view.findViewById(R.id.txtTickets)
        txtTicketName= view.findViewById(R.id.txtTicketName)
        txtTicketRate = view.findViewById(R.id.txtTicketRate)
        txtQty= view.findViewById(R.id.txtQty)
        txtTotalAmount = view.findViewById(R.id.totalAmount)
        editTextTickets = view.findViewById(R.id.editTextTickets)
        editTextTickets.post { editTextTickets.selectAll() }

        val billingLang = sessionManager.getBillingSelectedLanguage()
        val appLang = sessionManager.getSelectedLanguage()

        val currentLang = if (!billingLang.isNullOrEmpty()) billingLang else appLang




        btnBack = view.findViewById(R.id.btnBack)
        btnClear = view.findViewById(R.id.btnClear)
        icClose = view.findViewById(R.id.imgClose)
        btnDone = view.findViewById(R.id.btnDone)
        txtTickets.text = getString(R.string.no_of_tickets)
        btnDone.text  = getString(R.string.done)
        val text = getString(R.string.no_of_tickets)
        txtTickets.text = wrapTextByLength(text, 20)

        txtTicketName.text = when (currentLang) {
            LANGUAGE_ENGLISH -> ticketName
            LANGUAGE_MALAYALAM -> ticketNameMa
            LANGUAGE_TAMIL -> ticketNameTa
            LANGUAGE_KANNADA -> ticketNameKa
            LANGUAGE_TELUGU -> ticketNameTe
            LANGUAGE_HINDI -> ticketNameHi
            LANGUAGE_SINHALA-> ticketNameSi
            LANGUAGE_PUNJABI -> ticketNamePa
            LANGUAGE_MARATHI -> ticketNameMr
            else -> ticketName
        }
        txtTicketName.text = when (sessionManager.getSelectedLanguage()) {
            LANGUAGE_ENGLISH -> ticketName
            LANGUAGE_MALAYALAM -> ticketNameMa
            LANGUAGE_TAMIL -> ticketNameTa
            LANGUAGE_KANNADA -> ticketNameKa
            LANGUAGE_TELUGU -> ticketNameTe
            LANGUAGE_HINDI -> ticketNameHi
            LANGUAGE_SINHALA-> ticketNameSi
            LANGUAGE_PUNJABI -> ticketNamePa
            LANGUAGE_MARATHI -> ticketNameMr
            else -> ticketName
        }





        val formattedAmount = String.format(Locale.ENGLISH, "%.2f", ticketRate)
        txtTicketRate.text = "Rs. $formattedAmount /-"

        editTextTickets.inputType = 0
        editTextTickets.requestFocus()
        lifecycleScope.launch {
            val cartItem = ticketRepository.getCartItemByTicketId(ticketId)
            cartItem?.let { item ->
                withContext(Dispatchers.Main) {
                    editTextTickets.setText(item.daQty.toString())
                    updateTotalAmount(item.daQty)
                }
            } ?: run {
                withContext(Dispatchers.Main) {
                    editTextTickets.setText("1")
                    updateTotalAmount(1)
                }
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
                    editTextTickets.setText("")
                }
                appendToFocusedEditText((v as TextView).text.toString())
            }
        }


        editTextTickets.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val quantity = s?.toString()?.toIntOrNull() ?: 0
                updateTotalAmount(quantity)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        btnBack.setOnClickListener {
            removeLastCharacterFromFocusedEditText()
        }

        icClose.setOnClickListener {
            editTextTickets.setText("1")
            firstClick = true
            dismiss()
        }


        btnDone.setOnClickListener {
            val quantity = editTextTickets.text.toString().toIntOrNull() ?: 0

            if (quantity <= 0) {
                Toast.makeText(requireContext(), "Please enter a valid quantity", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val cartItem = Ticket(
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
                ticketAmount = ticketRate,
                ticketTotalAmount = totalAmount.toDouble(),
                ticketCreatedDate = System.currentTimeMillis().toString(),
                ticketCreatedBy =0 ,
                ticketActive = true,
                daName = "",
                daRate = ticketRate,
                daQty = quantity,
                daTotalAmount = totalAmount.toDouble(),
                daPhoneNumber = "",
                daProofId = "",
                daProof = "",
                daImg = byteArrayOf(),
                daCustRefNo = "",
                daNpciTransId = ""
            )

            lifecycleScope.launch {
                ticketRepository.insertCartItem(cartItem)
                listener?.onTicketAdded()
                firstClick = true
                dismiss()
            }
        }



        btnClear.setOnClickListener {
            clearFocusedEditText()
        }

        editTextTickets.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                editTextTickets.requestFocus()
                hideSoftKeyboard()
            }
            true
        }
    }

    fun wrapTextByLength(text: String, maxChars: Int = 20): String {
        val builder = StringBuilder()
        var index = 0
        while (index < text.length) {
            val end = (index + maxChars).coerceAtMost(text.length)
            builder.append(text.substring(index, end))
            if (end < text.length) builder.append("\n")
            index = end
        }
        return builder.toString()
    }

    @SuppressLint("SetTextI18n")
    private fun appendToFocusedEditText(text: String) {
        val currentText = editTextTickets.text.toString()
        if (currentText.isEmpty() && text == "0") {
            return
        }

        editTextTickets.setText(currentText + text)
        editTextTickets.setSelection(editTextTickets.text.length)
    }


    private fun removeLastCharacterFromFocusedEditText() {
        val currentText = editTextTickets.text?.toString().orEmpty()

        if (currentText.isNotEmpty()) {
            val updatedText = currentText.dropLast(1)
            editTextTickets.setText(updatedText)
            editTextTickets.setSelection(updatedText.length)
        }
    }




    private fun clearFocusedEditText() {
        editTextTickets.text!!.clear()
        editTextTickets.requestFocus()
        updateTotalAmount(0)
    }


    private fun hideSoftKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(editTextTickets.windowToken, 0)
    }


    @SuppressLint("DefaultLocale", "SetTextI18n")
    private fun updateTotalAmount(quantity: Int) {
        if (quantity == 0) {
            txtQty.text = getString(R.string.txt_amount)+" :"
            txtTotalAmount.visibility = View.GONE
            return
        } else {
            txtQty.visibility = View.VISIBLE
            txtTotalAmount.visibility = View.VISIBLE
        }

        totalAmount = (ticketRate * quantity).toString()
        val formattedRate = String.format(Locale.ENGLISH, "%.2f", ticketRate)
        txtQty.text = getString(R.string.txt_amount)+" : " + formattedRate + " x " + quantity

        val formattedTotal = String.format(Locale.ENGLISH, "%.2f", totalAmount.toDouble())
        txtTotalAmount.text = "Rs. $formattedTotal/-"
    }



}