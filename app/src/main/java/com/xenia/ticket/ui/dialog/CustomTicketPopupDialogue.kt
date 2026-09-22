


package com.xenia.ticket.ui.dialog

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xenia.ticket.ui.dialog.CustomTicketAllocationpopupDialogue
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
import com.xenia.ticket.ui.adapter.DateChipAdapter
import com.xenia.ticket.ui.adapter.ShowScheduleAdapter
import com.xenia.ticket.utils.common.CommonMethod.dismissLoader
import com.xenia.ticket.utils.common.CommonMethod.formatTime
import com.xenia.ticket.utils.common.CommonMethod.getTodayDay
import com.xenia.ticket.utils.common.CommonMethod.showLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.getValue

class CustomTicketPopupDialogue : DialogFragment() {

    private lateinit var txtTicketName: TextView
    private lateinit var txtComboTicketName: TextView
    private lateinit var txtDesc: TextView
    private lateinit var txtTicketRate: TextView
    private lateinit var txtTicketChildRate: TextView
    private lateinit var txtTicketRateAmount: TextView
    private lateinit var txtTicketChildRateAmount: TextView
    private lateinit var txtQty: TextView
    private lateinit var txtTotalAmount: TextView
    private lateinit var relTicket: View
    private lateinit var relChild: View
    private lateinit var editTextTickets: EditText
    private lateinit var editTextChildTickets: EditText
    private lateinit var backCallback: OnBackPressedCallback
    private lateinit var icClose: RelativeLayout
    private lateinit var btnDone: MaterialButton

    // Date / time slot recyclerviews
    private lateinit var rvDateSlots: RecyclerView
    private lateinit var rvTimeSlots: RecyclerView
    private lateinit var linDateSection: View
    private lateinit var linTimeSection: View
    private lateinit var txtNoSlotsMessage: TextView
    private lateinit var timeAdapter: ShowScheduleAdapter
    private var allSchedules: List<ShowScheduleResponse> = emptyList()

    // Adult keypad + stepper
    private lateinit var btnAdultMinus: ImageView
    private lateinit var btnAdultPlus: ImageView
    private lateinit var btnBackAdult: ImageView
    private lateinit var txtAdultCardTotal: TextView
    private lateinit var txtAdultCardQty: TextView

    // Child keypad + stepper
    private lateinit var btnChildMinus: ImageView
    private lateinit var btnChildPlus: ImageView
    private lateinit var btnBackChild: ImageView
    private lateinit var txtChildCardTotal: TextView
    private lateinit var txtChildCardQty: TextView

    private var firstClickAdult: Boolean = true
    private var firstClickChild: Boolean = true

    private val ticketRepository: OrderRepository by inject()
    private val activeTicketRepository: TicketRepository by inject()
    private val sessionManager: SessionManager by inject()

    private var ticketId: Int = 0
    private var showId: Int = 0
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
    private var ticketDesc: String = ""
    private var ticketChild: Boolean = false
    private var listener: OnTicketClickListener? = null
    var selectedSchedule: ShowScheduleResponse? = null
    private var selectedScheduleDate: String = ""   // NEW: tracks the date of the currently selected schedule
    private var comboShowId: Int? = null

    // ---- Pending values held while SeatSelectionActivity is open, consumed once it returns ----
    private var pendingQuantity = 0
    private var pendingChildQuantity = 0
    private var pendingFinalChildRate = 0.0
    private var currentlySelectedSeats: String? = null

    // Must be a property (registered at construction time), not created inside onViewCreated.
    private val seatAllocationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult

        val seats = result.data
            ?.getStringArrayListExtra(CustomTicketAllocationpopupDialogue.EXTRA_SELECTED_SEATS)
            ?: arrayListOf()

        if (seats.isEmpty()) {
            Toast.makeText(requireContext(), "No seats were selected", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }

        saveCartItem(
            quantity = pendingQuantity,
            childQuantity = pendingChildQuantity,
            finalChildRate = pendingFinalChildRate,
            selectedSeatNumbers = seats
        )
    }

    fun setListener(listener: OnTicketClickListener) {
        this.listener = listener
    }

    fun setData(
        ticketId: Int,
        showId: Int,
        ticketName: String,
        ticketNameMa: String,
        ticketNameTa: String,
        ticketNameKa: String,
        ticketNameTe: String,
        ticketNameHi: String,
        ticketNameSi: String,
        ticketNamePa: String,
        ticketNameMr: String,
        ticketDesc: String,
        ticketCategoryId: Int,
        ticketCompanyId: Int,
        ticketRate: Double,
        ticketChildRate: Double,
        ticketCombo: Boolean,
        ticketType: String,
        ticketChild: Boolean
    ) {
        this.ticketId = ticketId
        this.showId = showId
        this.ticketName = ticketName
        this.ticketNameMa = ticketNameMa
        this.ticketNameTa = ticketNameTa
        this.ticketNameKa = ticketNameKa
        this.ticketNameTe = ticketNameTe
        this.ticketNameHi = ticketNameHi
        this.ticketNameSi = ticketNameSi
        this.ticketNameMr = ticketNameMr
        this.ticketNamePa = ticketNamePa
        this.ticketDesc = ticketDesc
        this.ticketRate = ticketRate
        this.ticketChildRate = ticketChildRate
        this.ticketCompanyId = ticketCompanyId
        this.ticketCategoryId = ticketCategoryId
        this.ticketCombo = ticketCombo
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
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        backCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                dismissLoader()
                dismiss()
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(this, backCallback)
    }

    override fun onStop() {
        super.onStop()
        dismissLoader()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.custom_ticket_dialogue, container, false)
    }

    private var currentlySelectedSeatIds: String? = null   // NEW

    private fun setupSeatAllocationResultListener() {
        childFragmentManager.setFragmentResultListener(
            CustomTicketAllocationpopupDialogue.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, result ->

            val cancelled = result.getBoolean(
                CustomTicketAllocationpopupDialogue.EXTRA_CANCELLED, false
            )
            if (cancelled) {
                Log.d("SEAT_RESULT", "Seat allocation cancelled")
                return@setFragmentResultListener
            }

            val selectedSeats =
                result.getStringArrayList(CustomTicketAllocationpopupDialogue.EXTRA_SELECTED_SEATS)
                    ?: arrayListOf()

            val selectedSeatIds =
                result.getIntegerArrayList(CustomTicketAllocationpopupDialogue.EXTRA_SELECTED_SEAT_IDS)   // NEW
                    ?: arrayListOf()

            val returnedScheduleId =
                result.getInt(CustomTicketAllocationpopupDialogue.EXTRA_SCHEDULE_ID, 0)

            Log.d("SEAT_RESULT", "Seats=$selectedSeats, SeatIds=$selectedSeatIds, scheduleId=$returnedScheduleId")

            currentlySelectedSeats = selectedSeats.joinToString(",")
            currentlySelectedSeatIds = selectedSeatIds.joinToString(",")   // NEW

            saveCartItem(
                quantity = pendingQuantity,
                childQuantity = pendingChildQuantity,
                finalChildRate = pendingFinalChildRate,
                selectedSeatNumbers = selectedSeats,
                selectedSeatIdList = selectedSeatIds   // NEW
            )
        }
    }

    @SuppressLint("SetTextI18n", "DefaultLocale", "ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSeatAllocationResultListener()

        txtTicketName = view.findViewById(R.id.txtTicketName)
        txtComboTicketName = view.findViewById(R.id.txtComboTicketName)
        txtDesc = view.findViewById(R.id.txtDesc)
        txtTicketRate = view.findViewById(R.id.txtTicketRate)
        txtTicketRateAmount = view.findViewById(R.id.txtTicketRateAmount)
        txtTicketChildRateAmount = view.findViewById(R.id.txtTicketChildRateAmount)
        relTicket = view.findViewById(R.id.relTicket)
        relChild = view.findViewById(R.id.relChild)
        txtTicketChildRate = view.findViewById(R.id.txtTicketChildRate)
        txtQty = view.findViewById(R.id.txtQty)
        txtTotalAmount = view.findViewById(R.id.totalAmount)
        editTextTickets = view.findViewById(R.id.editTextTickets)
        editTextChildTickets = view.findViewById(R.id.editTextChildTickets)

        btnAdultMinus = view.findViewById(R.id.btnAdultMinus)
        btnAdultPlus = view.findViewById(R.id.btnAdultPlus)
        btnBackAdult = view.findViewById(R.id.btnBackAdult)
        txtAdultCardTotal = view.findViewById(R.id.txtAdultCardTotal)
        txtAdultCardQty = view.findViewById(R.id.txtAdultCardQty)

        btnChildMinus = view.findViewById(R.id.btnChildMinus)
        btnChildPlus = view.findViewById(R.id.btnChildPlus)
        btnBackChild = view.findViewById(R.id.btnBackChild)
        txtChildCardTotal = view.findViewById(R.id.txtChildCardTotal)
        txtChildCardQty = view.findViewById(R.id.txtChildCardQty)

        rvDateSlots = view.findViewById(R.id.rvDateSlots)
        rvTimeSlots = view.findViewById(R.id.rvTimeSlots)
        linDateSection = view.findViewById(R.id.linDateSection)
        linTimeSection = view.findViewById(R.id.linTimeSection)
        txtNoSlotsMessage = view.findViewById(R.id.txtNoSlotsMessage)

        applyTicketUIRules()

        val billingLang = sessionManager.getBillingSelectedLanguage()
        val appLang = sessionManager.getSelectedLanguage()
        val currentLang = (if (billingLang.isNotEmpty()) billingLang else appLang).lowercase()

        if (currentLang == "ml") {
            txtNoSlotsMessage.text = "ഈ തീയതിയിൽ ഷോ സമയങ്ങൾ ലഭ്യമല്ല"
        } else {
            txtNoSlotsMessage.text = "No time slots available for this date"
        }

        icClose = view.findViewById(R.id.imgClose)
        btnDone = view.findViewById(R.id.btnDones)

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


        applyTicketUIRules()

        // Default: Hide date and time sections
        linDateSection.visibility = View.GONE
        linTimeSection.visibility = View.GONE

        // TIME SLOT ADAPTER (shared by SHOW + COMBO)
        timeAdapter = ShowScheduleAdapter(emptyList()) { selectedItem ->
            selectedSchedule = selectedItem
        }
        rvTimeSlots.layoutManager = GridLayoutManager(requireContext(), 5)
        rvTimeSlots.adapter = timeAdapter

        if (ticketType.equals("SHOW", ignoreCase = true)) {
            txtComboTicketName.visibility = View.GONE
            txtDesc.visibility = View.VISIBLE
            txtTicketName.text = ticketName

            lifecycleScope.launch {
                try {
                    Log.d("SCHEDULE_LOAD", "Loading schedules for showId: $showId")
                    showLoader(requireContext(), "Loading schedules...")
                    val today = Calendar.getInstance()

                    val dayFormat = SimpleDateFormat(
                        "EEEE",
                        Locale.ENGLISH
                    )

                    val dateFormat = SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.ENGLISH
                    )

                    val day = dayFormat.format(today.time)
                    val date = dateFormat.format(today.time)

                    val schedules = withContext(Dispatchers.IO) {
                        activeTicketRepository.getSchedules(
                            showId,
                            day,
                            date
                        )
                    }

                    val existingItem = withContext(Dispatchers.IO) {
                        ticketRepository.getCartItemByTicketId(ticketId)
                    }
                    currentlySelectedSeats = existingItem?.selectedSeats
                    Log.d("SCHEDULE_LOAD", "Schedules loaded: ${schedules.size}")

                    if (schedules.isNotEmpty()) {
                        allSchedules = schedules
                        linDateSection.visibility = View.VISIBLE
                        linTimeSection.visibility = View.VISIBLE
                        rvTimeSlots.visibility = View.VISIBLE
                        txtNoSlotsMessage.visibility = View.GONE
                        bindDateAndTimeSlots(schedules, existingItem?.scheduleId)
                    } else {
                        linDateSection.visibility = View.VISIBLE
                        linTimeSection.visibility = View.VISIBLE
                        rvTimeSlots.visibility = View.GONE
                        txtNoSlotsMessage.visibility = View.VISIBLE
                    }

                } catch (_: Exception) {
                    linDateSection.visibility = View.VISIBLE
                    linTimeSection.visibility = View.VISIBLE
                    rvTimeSlots.visibility = View.GONE
                    txtNoSlotsMessage.visibility = View.VISIBLE
                } finally {
                    dismissLoader()
                }
            }
        }

        if (ticketCombo) {
            lifecycleScope.launch {
                try {
                    Log.d("SCHEDULE_LOAD", "Loading combo result for ticketId: $ticketId")
                    showLoader(requireContext(), "Loading schedules...")

                    val result = withContext(Dispatchers.IO) {
                        activeTicketRepository.getComboResult(ticketId)
                    }
                    comboShowId = result.showId
                    txtComboTicketName.visibility = View.VISIBLE
                    txtComboTicketName.text = result.names.joinToString(" | ")

                    if (result.showId != null) {
                        txtDesc.visibility = View.VISIBLE
                        txtTicketName.text = ticketName

                        val today = Calendar.getInstance()

                        val dayFormat = SimpleDateFormat(
                            "EEEE",
                            Locale.ENGLISH
                        )

                        val dateFormat = SimpleDateFormat(
                            "yyyy-MM-dd",
                            Locale.ENGLISH
                        )

                        val day = dayFormat.format(today.time)
                        val date = dateFormat.format(today.time)

                        val schedules = withContext(Dispatchers.IO) {
                            activeTicketRepository.getSchedules(
                                result.showId,
                                day,
                                date
                            )
                        }

                        val existingItem = withContext(Dispatchers.IO) {
                            ticketRepository.getCartItemByTicketId(ticketId)
                        }
                        currentlySelectedSeats = existingItem?.selectedSeats
                        Log.d("SCHEDULE_LOAD", "Combo schedules loaded: ${schedules.size}")

                        if (schedules.isNotEmpty()) {
                            allSchedules = schedules
                            linDateSection.visibility = View.VISIBLE
                            linTimeSection.visibility = View.VISIBLE
                            rvTimeSlots.visibility = View.VISIBLE
                            txtNoSlotsMessage.visibility = View.GONE
                            bindDateAndTimeSlots(schedules, existingItem?.scheduleId)
                        } else {
                            linDateSection.visibility = View.VISIBLE
                            linTimeSection.visibility = View.VISIBLE
                            rvTimeSlots.visibility = View.GONE
                            txtNoSlotsMessage.visibility = View.VISIBLE
                        }
                    } else {
                        linDateSection.visibility = View.GONE
                        linTimeSection.visibility = View.GONE
                    }

                } catch (_: Exception) {
                    linDateSection.visibility = View.VISIBLE
                    linTimeSection.visibility = View.VISIBLE
                    rvTimeSlots.visibility = View.GONE
                    txtNoSlotsMessage.visibility = View.VISIBLE
                } finally {
                    dismissLoader()
                }
            }
        }

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

                updateAmounts()
            }
        }

        // =ADULT KEYPAD
        val adultNumberButtons = mapOf(
            R.id.btnOneAdult to "1", R.id.btnTwoAdult to "2", R.id.btnThreeAdult to "3",
            R.id.btnFourAdult to "4", R.id.btnFiveAdult to "5", R.id.btnSixAdult to "6",
            R.id.btnSevenAdult to "7", R.id.btnEightAdult to "8", R.id.btnNineAdult to "9",
            R.id.btnZeroAdult to "0"
        )

        adultNumberButtons.forEach { (id, digit) ->
            view.findViewById<TextView>(id).setOnClickListener {
                appendToEditText(editTextTickets, digit, isAdult = true)
            }
        }

        view.findViewById<TextView>(R.id.btnPlusTenAdult).setOnClickListener {
            addToEditText(editTextTickets, 10, isAdult = true)
        }

        btnBackAdult.setOnClickListener {
            removeLastCharacter(editTextTickets)
        }

        btnAdultMinus.setOnClickListener {
            addToEditText(editTextTickets, -1, isAdult = true)
        }

        btnAdultPlus.setOnClickListener {
            addToEditText(editTextTickets, 1, isAdult = true)
        }

        // ================= CHILD KEYPAD =================
        val childNumberButtons = mapOf(
            R.id.btnOneChild to "1", R.id.btnTwoChild to "2", R.id.btnThreeChild to "3",
            R.id.btnFourChild to "4", R.id.btnFiveChild to "5", R.id.btnSixChild to "6",
            R.id.btnSevenChild to "7", R.id.btnEightChild to "8", R.id.btnNineChild to "9",
            R.id.btnZeroChild to "0"
        )

        childNumberButtons.forEach { (id, digit) ->
            view.findViewById<TextView>(id).setOnClickListener {
                appendToEditText(editTextChildTickets, digit, isAdult = false)
            }
        }

        view.findViewById<TextView>(R.id.btnPlusTenChild).setOnClickListener {
            addToEditText(editTextChildTickets, 10, isAdult = false)
        }

        btnBackChild.setOnClickListener {
            removeLastCharacter(editTextChildTickets)
        }

        btnChildMinus.setOnClickListener {
            addToEditText(editTextChildTickets, -1, isAdult = false)
        }

        btnChildPlus.setOnClickListener {
            addToEditText(editTextChildTickets, 1, isAdult = false)
        }

        icClose.setOnClickListener {
            dismissLoader()
            dismiss()
        }

        btnDone.setOnClickListener {
            val childOnlyMode = ticketType.equals("TICKET", true) && ticketChild

            val quantityInput = editTextTickets.text.toString().toIntOrNull() ?: 0
            val childQuantityInput = editTextChildTickets.text.toString().toIntOrNull() ?: 0

            val quantity = quantityInput
            val totalQty = quantity + childQuantityInput

            Log.d(
                "TICKET_DONE",
                "childOnlyMode=$childOnlyMode, quantityInput=$quantityInput, childQuantityInput=$childQuantityInput, " +
                        "quantity=$quantity, totalQty=$totalQty"
            )

            if (totalQty <= 0) {
                Log.d("TICKET_DONE", "BLOCKED: totalQty <= 0")
                Toast.makeText(requireContext(), "Please enter a valid quantity", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ---- Check if seat allocation is required ----
            val isSeatCheckRequired = ticketType.equals("SHOW", ignoreCase = true) || (ticketCombo && comboShowId != null)

            Log.d(
                "TICKET_DONE",
                "isSeatCheckRequired=$isSeatCheckRequired, ticketType=$ticketType, " +
                        "selectedSchedule=${selectedSchedule?.ScheduleId}"
            )

            if (isSeatCheckRequired && selectedSchedule == null) {
                Log.d("TICKET_DONE", "BLOCKED: seat check required but no schedule selected")
                Toast.makeText(requireContext(), "Please select a schedule", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isSeatCheckRequired) {
                val availableSeats = selectedSchedule?.AvailableSeats ?: 0

                Log.d("TICKET_DONE", "Checking seats: totalQty=$totalQty, availableSeats=$availableSeats")

                if (totalQty > availableSeats) {
                    Log.d("TICKET_DONE", "BLOCKED: totalQty ($totalQty) > availableSeats ($availableSeats)")
                    Toast.makeText(
                        requireContext(),
                        "Only $availableSeats seats available",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
            }

            val finalChildRate = if (childOnlyMode) ticketRate else ticketChildRate

            Log.d("TICKET_DONE", "finalChildRate=$finalChildRate")

            //  Redirect to seat allocation popup instead of saving directly
            if (isSeatCheckRequired) {
                pendingQuantity = quantity
                pendingChildQuantity = childQuantityInput
                pendingFinalChildRate = finalChildRate

                val schedule = selectedSchedule!! // already null-checked above

                Log.d(
                    "TICKET_DONE",
                    "OPENING SEAT ALLOCATION POPUP -> scheduleId=${schedule.ScheduleId}, screenId=${schedule.ScreenId}, " +
                            "screenName=${schedule.ScreenName}, availableSeats=${schedule.AvailableSeats}, " +
                            "showDay=${schedule.ShowDay}, startTime=${formatTime(schedule.StartTime)}, pricePerSeat=$ticketRate"
                )

                CustomTicketAllocationpopupDialogue.newInstance(
                    ticketId = ticketId,
                    scheduleId = schedule.ScheduleId,
                    screenId = schedule.ScreenId,
                    screenName = schedule.ScreenName,
                    availableSeats = schedule.AvailableSeats,
                    requestedQuantity = totalQty,
                    showDay = schedule.ShowDay,
                    startTime = formatTime(schedule.StartTime),
                    pricePerSeat = ticketRate,
                    initialSelectedSeats = currentlySelectedSeats
                ).show(childFragmentManager, "seat_allocation")

                Log.d("TICKET_DONE", "seat_allocation dialog show() called")

                return@setOnClickListener
            }

            Log.d(
                "TICKET_DONE",
                "SAVING DIRECTLY -> quantity=$quantity, childQuantity=$childQuantityInput, finalChildRate=$finalChildRate"
            )

            // no seat allocation needed -> save straight away, same as before
            saveCartItem(
                quantity = quantity,
                childQuantity = childQuantityInput,
                finalChildRate = finalChildRate,
                selectedSeatNumbers = emptyList()
            )
        }
    }


    private fun saveCartItem(
        quantity: Int,
        childQuantity: Int,
        finalChildRate: Double,
        selectedSeatNumbers: List<String>,
        selectedSeatIdList: List<Int> = emptyList()
    ) {
        val adultTotal = ticketRate * quantity
        val childTotal = finalChildRate * childQuantity
        val grandTotal = adultTotal + childTotal

        val cartItem = Orders(
            ticketId = ticketId,
            showId = if (ticketCombo) (comboShowId ?: 0) else showId,
            ticketName = ticketName,
            ticketNameMa = ticketNameMa,
            ticketNameTa = ticketNameTa,
            ticketNameTe = ticketNameTe,
            ticketNameKa = ticketNameKa,
            ticketNameHi = ticketNameHi,
            ticketNamePa = ticketNamePa,
            ticketNameMr = ticketNameMr,
            ticketNameSi = ticketNameSi,
            ticketDesc = ticketDesc,
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
            ticketChildQty = childQuantity,
            ticketTotalAmount = grandTotal,
            daPhoneNumber = "",
            daCustRefNo = "",
            daNpciTransId = "",
            daProofId = "",
            daProof = "",
            daImg = byteArrayOf(),
            screenId = selectedSchedule?.ScreenId ?: 0,
            scheduleId = selectedSchedule?.ScheduleId ?: 0,
            scheduleDay = selectedSchedule?.ShowDay ?: "",
            scheduleTime = selectedSchedule?.let { formatTime(it.StartTime) } ?: "",
            scheduleDate = selectedScheduleDate,   // NEW: date of the selected schedule
            screenName = selectedSchedule?.ScreenName ?: "",
            ticketChild = ticketChild,
            selectedSeats = selectedSeatNumbers.joinToString(","),
            selectedSeatIds = selectedSeatIdList.joinToString(",")
        )

        lifecycleScope.launch {
            Log.d("CART_ACTION", "Inserting item to cart: ticketId=$ticketId, seats=${cartItem.selectedSeats}, scheduleDate=${cartItem.scheduleDate}")
            ticketRepository.insertCartItem(cartItem)
            Log.d("CART_ACTION", "Cart insertion successful for ticketId=$ticketId")
            listener?.onTicketAdded(ticketId)
            dismiss()
        }
    }


    private fun bindDateAndTimeSlots(
        schedules: List<ShowScheduleResponse>,
        existingScheduleId: Int?
    ) {

        val apiDateFormat =
            SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)

        val today = Calendar.getInstance()

        val fiveDates = mutableListOf<String>()

        for (i in 0..4) {

            val calendar =
                today.clone() as Calendar

            calendar.add(
                Calendar.DAY_OF_MONTH,
                i
            )

            val formattedDate =
                apiDateFormat.format(calendar.time)

            fiveDates.add(formattedDate)

            Log.d(
                "SCHEDULE_DATE",
                "Date chip [$i] -> $formattedDate"
            )
        }


        val dateAdapter =
            DateChipAdapter(fiveDates) { selectedDate ->

                try {

                    selectedScheduleDate = selectedDate   // NEW: remember the date this chip represents

                    Log.d(
                        "SCHEDULE_API",
                        "========================================"
                    )

                    Log.d(
                        "SCHEDULE_API",
                        "DATE CHIP CLICKED"
                    )

                    Log.d(
                        "SCHEDULE_API",
                        "ticketId = $ticketId"
                    )

                    Log.d(
                        "SCHEDULE_API",
                        "selectedDate = $selectedDate"
                    )


                    val selectedCalendar =
                        Calendar.getInstance()

                    val parsedDate =
                        apiDateFormat.parse(selectedDate)

                    if (parsedDate == null) {

                        Log.e(
                            "SCHEDULE_API",
                            "ERROR: Unable to parse selectedDate=$selectedDate"
                        )

                        return@DateChipAdapter
                    }

                    selectedCalendar.time =
                        parsedDate

                    val dayFormat =
                        SimpleDateFormat(
                            "EEEE",
                            Locale.ENGLISH
                        )

                    val day =
                        dayFormat.format(
                            selectedCalendar.time
                        )

                    Log.d(
                        "SCHEDULE_API",
                        "Calculated day = $day"
                    )

                    Log.d(
                        "SCHEDULE_API",
                        "API PARAMETERS -> ticketId=$ticketId, day=$day, date=$selectedDate"
                    )


                    lifecycleScope.launch {

                        try {

                            Log.d("SCHEDULE_API", "Showing loader for date chip click")
                            showLoader(
                                requireContext(),
                                "Loading schedules..."
                            )

                            val selectedSchedules =
                                withContext(Dispatchers.IO) {

                                    Log.d(
                                        "SCHEDULE_API",
                                        "CALLING API -> ticketId=$ticketId, day=$day, date=$selectedDate"
                                    )

                                    val result =
                                        activeTicketRepository.getSchedules(
                                            if (ticketCombo) (comboShowId ?: 0) else showId,
                                            day,
                                            selectedDate
                                        )

                                    Log.d(
                                        "SCHEDULE_API",
                                        "API RESPONSE -> count=${result.size}"
                                    )

                                    result.forEachIndexed { index, schedule ->

                                        Log.d(
                                            "SCHEDULE_API",
                                            "Schedule[$index] -> " +
                                                    "ScheduleId=${schedule.ScheduleId}, " +
                                                    "ShowDay=${schedule.ShowDay}, " +
                                                    "StartTime=${schedule.StartTime}, " +
                                                    "AvailableSeats=${schedule.AvailableSeats}, " +
                                                    "ScreenId=${schedule.ScreenId}, " +
                                                    "ScreenName=${schedule.ScreenName}"
                                        )
                                    }

                                    result
                                }


                            allSchedules =
                                selectedSchedules

                            timeAdapter.updateData(
                                selectedSchedules
                            )

                            if (selectedSchedules.isEmpty()) {
                                txtNoSlotsMessage.visibility = View.VISIBLE
                                rvTimeSlots.visibility = View.GONE
                            } else {
                                txtNoSlotsMessage.visibility = View.GONE
                                rvTimeSlots.visibility = View.VISIBLE
                            }

                            selectedSchedule =
                                selectedSchedules.firstOrNull {
                                    it.AvailableSeats > 0
                                }

                            Log.d(
                                "SCHEDULE_API",
                                "Selected schedule = ${selectedSchedule?.ScheduleId}"
                            )

                            Log.d(
                                "SCHEDULE_API",
                                "Available seats = ${selectedSchedule?.AvailableSeats}"
                            )

                        } catch (e: Exception) {

                            Log.e(
                                "SCHEDULE_API",
                                "API ERROR -> ${e.message}",
                                e
                            )

                            timeAdapter.updateData(
                                emptyList()
                            )

                            txtNoSlotsMessage.visibility = View.VISIBLE
                            rvTimeSlots.visibility = View.GONE
                            selectedSchedule = null

                            Toast.makeText(
                                requireContext(),
                                "Unable to load schedules",
                                Toast.LENGTH_SHORT
                            ).show()

                        } finally {

                            dismissLoader()

                            Log.d(
                                "SCHEDULE_API",
                                "API CALL FINISHED"
                            )

                            Log.d(
                                "SCHEDULE_API",
                                "========================================"
                            )
                        }
                    }

                } catch (e: Exception) {

                    Log.e(
                        "SCHEDULE_API",
                        "DATE PROCESSING ERROR -> ${e.message}",
                        e
                    )
                }
            }


        rvDateSlots.layoutManager =
            LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
            )

        rvDateSlots.adapter =
            dateAdapter


        val currentDate =
            apiDateFormat.format(today.time)

        val dayFormat =
            SimpleDateFormat(
                "EEEE",
                Locale.ENGLISH
            )

        val currentDay =
            dayFormat.format(today.time)

        selectedScheduleDate = currentDate   // NEW: default to today's date until user picks a chip

        Log.d(
            "SCHEDULE_API",
            "========================================"
        )

        Log.d(
            "SCHEDULE_API",
            "LOADING TODAY SCHEDULE"
        )

        Log.d(
            "SCHEDULE_API",
            "ticketId = $ticketId"
        )

        Log.d(
            "SCHEDULE_API",
            "currentDay = $currentDay"
        )

        Log.d(
            "SCHEDULE_API",
            "currentDate = $currentDate"
        )


        lifecycleScope.launch {

            try {

                Log.d("SCHEDULE_API", "Showing loader for TODAY schedule")
                showLoader(
                    requireContext(),
                    "Loading schedules..."
                )

                val todaySchedules =
                    withContext(Dispatchers.IO) {

                        Log.d(
                            "SCHEDULE_API",
                            "CALLING TODAY API -> ticketId=$ticketId, day=$currentDay, date=$currentDate"
                        )

                        val result =
                            activeTicketRepository.getSchedules(
                                if (ticketCombo) (comboShowId ?: 0) else showId,
                                currentDay,
                                currentDate
                            )

                        Log.d(
                            "SCHEDULE_API",
                            "TODAY API RESPONSE -> count=${result.size}"
                        )

                        result.forEachIndexed { index, schedule ->

                            Log.d(
                                "SCHEDULE_API",
                                "TodaySchedule[$index] -> " +
                                        "ScheduleId=${schedule.ScheduleId}, " +
                                        "ShowDay=${schedule.ShowDay}, " +
                                        "StartTime=${schedule.StartTime}, " +
                                        "AvailableSeats=${schedule.AvailableSeats}, " +
                                        "ScreenId=${schedule.ScreenId}, " +
                                        "ScreenName=${schedule.ScreenName}"
                            )
                        }

                        result
                    }

                allSchedules =
                    todaySchedules



                selectedSchedule =
                    existingScheduleId
                        ?.let { scheduleId ->

                            Log.d(
                                "SCHEDULE_API",
                                "Trying to restore scheduleId=$scheduleId"
                            )

                            todaySchedules.find {
                                it.ScheduleId == scheduleId
                            }
                        }
                        ?: todaySchedules.firstOrNull {
                            it.AvailableSeats > 0
                        }

                Log.d(
                    "SCHEDULE_API",
                    "Final selectedScheduleId = ${selectedSchedule?.ScheduleId}"
                )

                Log.d(
                    "SCHEDULE_API",
                    "Final selectedAvailableSeats = ${selectedSchedule?.AvailableSeats}"
                )



                timeAdapter.updateData(
                    todaySchedules
                )

                if (todaySchedules.isEmpty()) {
                    txtNoSlotsMessage.visibility = View.VISIBLE
                    rvTimeSlots.visibility = View.GONE
                } else {
                    txtNoSlotsMessage.visibility = View.GONE
                    rvTimeSlots.visibility = View.VISIBLE
                }

                selectedSchedule?.let {

                    timeAdapter.setSelectedByScheduleId(
                        it.ScheduleId
                    )
                }

            } catch (e: Exception) {

                Log.e(
                    "SCHEDULE_API",
                    "TODAY API ERROR -> ${e.message}",
                    e
                )

                timeAdapter.updateData(
                    emptyList()
                )

                txtNoSlotsMessage.visibility = View.VISIBLE
                rvTimeSlots.visibility = View.GONE
                selectedSchedule = null

            } finally {

                dismissLoader()

                Log.d(
                    "SCHEDULE_API",
                    "TODAY API CALL FINISHED"
                )

                Log.d(
                    "SCHEDULE_API",
                    "========================================"
                )
            }
        }
    }
    @SuppressLint("SetTextI18n")
    private fun applyTicketUIRules() {
        val childOnlyMode = isChildOnlyMode()

        val showAdult = true
        val showChild = true

        relTicket.visibility = View.VISIBLE
        relChild.visibility = View.VISIBLE

        if (showAdult && showChild) {
            val formattedAmount = String.format(Locale.ENGLISH, "%.2f", ticketRate)
            txtTicketRate.text = "Adult Tickets"
            txtTicketRateAmount.text = "Rs. $formattedAmount / per ticket"

            val formattedChildAmount = String.format(Locale.ENGLISH, "%.2f", ticketChildRate)
            txtTicketChildRate.text = "Child Tickets"
            txtTicketChildRateAmount.text = "Rs. $formattedChildAmount / per ticket"
        } else if (showAdult) {
            txtTicketRate.text = getString(R.string.no_of_ticket)
        } else {
            txtTicketChildRate.text = getString(R.string.no_of_ticket)
        }

        if (childOnlyMode) {
            editTextTickets.setText("0")
            editTextChildTickets.setText("1")
        } else {
            if (editTextTickets.text.isNullOrEmpty()) {
                editTextTickets.setText("1")
            }
            if (editTextChildTickets.text.isNullOrEmpty()) {
                editTextChildTickets.setText("0")
            }
        }

        updateAmounts()
    }

    fun splitTextByWords(text: String, maxCharsPerLine: Int, maxLines: Int): String {
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

    private fun appendToEditText(editText: EditText, digit: String, isAdult: Boolean) {
        val isFirstClick = if (isAdult) firstClickAdult else firstClickChild

        if (isFirstClick) {
            editText.setText(digit)
        } else {
            val currentText = editText.text.toString()
            editText.setText(if (currentText == "0") digit else currentText + digit)
        }

        if (isAdult) firstClickAdult = false else firstClickChild = false

        editText.setSelection(editText.text.length)
        updateAmounts()
    }

    private fun addToEditText(editText: EditText, delta: Int, isAdult: Boolean) {
        val current = editText.text.toString().toIntOrNull() ?: 0
        val updated = (current + delta).coerceAtLeast(0)
        editText.setText(updated.toString())
        editText.setSelection(editText.text.length)

        if (isAdult) firstClickAdult = false else firstClickChild = false

        updateAmounts()
    }

    private fun removeLastCharacter(editText: EditText) {
        val currentText = editText.text?.toString().orEmpty()

        if (currentText.isNotEmpty()) {
            val updatedText = currentText.dropLast(1)
            editText.setText(updatedText)
            editText.setSelection(updatedText.length)
        }

        updateAmounts()
    }

    private fun isChildOnlyMode(): Boolean {
        return ticketChild
    }

    @SuppressLint("DefaultLocale", "SetTextI18n")
    private fun updateAmounts() {

        val childOnlyMode = isChildOnlyMode()

        val adultQtyInput = editTextTickets.text.toString().toIntOrNull() ?: 0
        val childQtyInput = editTextChildTickets.text.toString().toIntOrNull() ?: 0

        val adultQty = adultQtyInput
        val totalQty = adultQty + childQtyInput

        val isSeatCheckRequired = ticketType.equals("SHOW", true) || (ticketCombo && comboShowId != null)

        if (isSeatCheckRequired && selectedSchedule != null) {
            val availableSeats = selectedSchedule?.AvailableSeats ?: Int.MAX_VALUE

            if (totalQty > availableSeats) {
                Toast.makeText(
                    requireContext(),
                    "Maximum $availableSeats seats allowed",
                    Toast.LENGTH_SHORT
                ).show()

                val allowedAdult = minOf(adultQty, availableSeats)
                val allowedChild = minOf(childQtyInput, availableSeats - allowedAdult)

                editTextTickets.setText(allowedAdult.toString())
                editTextChildTickets.setText(allowedChild.toString())
                return
            }
        }

        val finalChildRate = if (childOnlyMode) ticketRate else ticketChildRate

        val adultTotal = ticketRate * adultQty
        val childTotal = finalChildRate * childQtyInput
        val grandTotal = adultTotal + childTotal
        totalAmount = grandTotal.toString()

        txtAdultCardTotal.text = "Rs. %.2f/-".format(adultTotal)
        txtAdultCardQty.text = adultQtyInput.toString()

        txtChildCardTotal.text = "Rs. %.2f/-".format(childTotal)
        txtChildCardQty.text = childQtyInput.toString()

        if (adultQty == 0 && childQtyInput == 0) {
            txtQty.text = getString(R.string.txt_amount) + " :"
            txtTotalAmount.visibility = View.GONE
            return
        }

        txtTotalAmount.visibility = View.VISIBLE

        val adultText = if (adultQty > 0) {
            "Adult ($adultQty x ${"%.2f".format(ticketRate)})"
        } else ""

        val childText = if (childQtyInput > 0) {
            "Child ($childQtyInput x ${"%.2f".format(finalChildRate)})"
        } else ""

        txtQty.text = when {
            adultQty > 0 && childQtyInput > 0 -> "$adultText +\n$childText"
            adultQty > 0 -> adultText
            else -> childText
        }

        txtTotalAmount.text = "Rs. %.2f/-".format(grandTotal)
    }
    private fun getDayAndDate(calendar: Calendar): Pair<String, String> {

        val dayFormat = SimpleDateFormat(
            "EEEE",
            Locale.ENGLISH
        )

        val dateFormat = SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.ENGLISH
        )

        return Pair(
            dayFormat.format(calendar.time),
            dateFormat.format(calendar.time)
        )
    }
}