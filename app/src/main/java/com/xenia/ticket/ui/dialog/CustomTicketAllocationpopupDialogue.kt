package com.xenia.ticket.ui.dialog

import android.app.Dialog
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.graphics.drawable.toDrawable
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xenia.ticket.R
import com.xenia.ticket.data.network.model.SeatAvailability
import com.xenia.ticket.data.network.service.ApiClient
import com.xenia.ticket.databinding.CustomTicketAllocationBinding
import com.xenia.ticket.databinding.ItemSeatBinding
import org.koin.android.ext.android.inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.xenia.ticket.utils.common.SessionManager
import com.xenia.ticket.utils.common.JwtUtils


// SEAT MAP ITEMS


sealed class SeatMapItem {
    data class RowLabel(val label: String) : SeatMapItem()
    data class SeatItem(val seat: SeatAvailability) : SeatMapItem()
    object Spacer : SeatMapItem()
}


// SEAT STATE


enum class SeatState {
    AVAILABLE,
    SELECTED,
    BOOKED
}

// DIALOG


class CustomTicketAllocationpopupDialogue : DialogFragment() {

    private var _binding: CustomTicketAllocationBinding? = null
    private val binding get() = _binding!!

    private val orderRepository: com.xenia.ticket.data.repository.OrderRepository by inject()
    private val sessionManager: SessionManager by inject()

    private lateinit var backCallback: OnBackPressedCallback
    private lateinit var seatAdapter: SeatAdapter

    // User selected seats
    private val selectedSeats = linkedSetOf<String>()
    private val seatIdMap = mutableMapOf<String, Int>()


    // ARGUMENTS


    private var ticketId: Int = 0
    private var scheduleId: Int = 0
    private var screenId: Int = 0
    private var screenName: String = ""
    private var showDay: String = ""
    private var startTime: String = ""
    private var pricePerSeat: Double = 0.0
    private var requestedQuantity: Int = 0
    private var availableSeatsFromArgs: Int = 0

    // API available seat count
    private var availableSeatCount: Int = 0
    private var showDate: String = ""


    // DIALOG


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        return Dialog(requireActivity(), theme).apply {

            requestWindowFeature(Window.FEATURE_NO_TITLE)

            window?.setBackgroundDrawable(
                Color.TRANSPARENT.toDrawable()
            )

            setCanceledOnTouchOutside(false)
            setCancelable(false)
        }
    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )

        backCallback = object : OnBackPressedCallback(true) {

            override fun handleOnBackPressed() {

                sendCancelResult()
            }
        }

        requireActivity()
            .onBackPressedDispatcher
            .addCallback(this, backCallback)
    }


    // CREATE VIEW


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = CustomTicketAllocationBinding.inflate(
            inflater,
            container,
            false
        )

        return binding.root
    }


    // VIEW CREATED


    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        super.onViewCreated(view, savedInstanceState)

        readArguments()

        bindHeader()

        setupBackButton()

        setupSeatRecyclerView()

        setupProceedButton()

        updateBottomBar()

        // Initialize previously selected seats if any
        initializePreviousSelection()

        // Load actual seats from API
        loadSeatAvailability()
    }

    private fun initializePreviousSelection() {
        val args = arguments ?: return
        val initial = args.getString(EXTRA_INITIAL_SELECTED_SEATS) ?: return

        if (initial.isNotEmpty()) {
            val seats = initial.split(",")
            selectedSeats.clear()
            selectedSeats.addAll(seats)
            updateBottomBar()
        }
    }


    // READ ARGUMENTS


    private fun readArguments() {

        val args = requireArguments()

        ticketId = args.getInt(EXTRA_TICKET_ID)

        scheduleId = args.getInt(EXTRA_SCHEDULE_ID)

        screenId = args.getInt(EXTRA_SCREEN_ID)

        screenName =
            args.getString(EXTRA_SCREEN_NAME).orEmpty()

        showDay =
            args.getString(EXTRA_SHOW_DAY).orEmpty()

        startTime =
            args.getString(EXTRA_START_TIME).orEmpty()

        pricePerSeat =
            args.getDouble(EXTRA_PRICE_PER_SEAT)

        requestedQuantity =
            args.getInt(EXTRA_REQUESTED_QUANTITY)

        availableSeatsFromArgs =
            args.getInt(EXTRA_AVAILABLE_SEATS)

        showDate =
            args.getString(EXTRA_SHOW_DATE).orEmpty()
    }


    // HEADER


    private fun bindHeader() {

        binding.titleText.text =
            getString(R.string.select_your_seats)

        binding.screenPill.text =
            screenName

        binding.timePill.text =
            "$showDay | $startTime"
    }


    // BACK BUTTON


    private fun setupBackButton() {

        binding.backButton.setOnClickListener {

            sendCancelResult()
        }
    }

    private fun sendCancelResult() {

        setFragmentResult(
            REQUEST_KEY,
            bundleOf(
                EXTRA_CANCELLED to true
            )
        )

        dismiss()
    }


    // SEAT RECYCLER VIEW


    private fun setupSeatRecyclerView() {

        // Max seats per row in image is Row G (23)
        // Col 0 = Label, Col 1..23 = Seats
        val spanCount = 24

        val layoutManager = GridLayoutManager(requireContext(), spanCount)
        binding.seatMapRecyclerView.layoutManager = layoutManager

        seatAdapter = SeatAdapter(
            items = emptyList(),
            selectedSeats = selectedSeats,
            requestedQuantity = requestedQuantity
        ) {
            updateBottomBar()
        }

        binding.seatMapRecyclerView.adapter = seatAdapter
    }


    // API


    private fun loadSeatAvailability() {

        showSeatLoading(true)

        viewLifecycleOwner.lifecycleScope.launch {

            try {

                val token = sessionManager.getToken()
                val companyId = token?.let { JwtUtils.getCompanyId(it) }

                if (companyId == null) {
                    Log.e(TAG, "Unable to resolve companyId from token")
                    showSeatLoading(false)
                    Toast.makeText(
                        requireContext(),
                        "Session error. Please login again.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }
                Log.d(
                    TAG,
                    "Loading seats: scheduleId=$scheduleId, companyId=$companyId,date=$showDate"
                )

                val seats =
                    ApiClient.apiService.getSeatAvailability(
                        scheduleId = scheduleId,
                        companyId = companyId,
                        date = showDate
                    )

                Log.d(
                    TAG,
                    "API success. Total seats=${seats.size}, filtering for screenId=$screenId"
                )

                // 1. Get other seats already in cart for this same schedule
                val otherCartItems = orderRepository.getOtherCartItemsForSchedule(scheduleId, ticketId)
                val seatsAlreadyInCart = otherCartItems.flatMap { it.selectedSeats?.split(",") ?: emptyList() }.toSet()

                // 2. Filter by ScreenId, remove duplicates, and mark seats as booked if already in cart
                val processedSeats = seats.filter { it.ScreenId == screenId }
                    .map { seat ->
                        val seatId = "${seat.RowName}-${seat.SeatNumber}"
                        if (seatsAlreadyInCart.contains(seatId)) {
                            seat.copy(IsBooked = true)
                        } else {
                            seat
                        }
                    }.filter { it.Price > 0 }


                seatIdMap.clear()
                processedSeats.forEach { seat ->
                    val label = "${seat.RowName}-${seat.SeatNumber}"
                    seatIdMap[label] = seat.SeatId   // ⚠️ confirm this field name matches your SeatAvailability model
                }
                // 3. Group by Row and build flat list with Labels and Spacers
                val mapItems = mutableListOf<SeatMapItem>()
                val rows = processedSeats.groupBy { it.RowName }.toSortedMap()

                rows.forEach { (rowName, rowSeats) ->
                    // Add Row Label
                    mapItems.add(SeatMapItem.RowLabel(rowName))

                    // Add Seats for this row
                    val sortedSeats = rowSeats.sortedBy { it.SeatNumber }
                    sortedSeats.forEach { seat ->
                        mapItems.add(SeatMapItem.SeatItem(seat))
                    }

                    // Fill remaining columns in the 24-col grid with spacers
                    val remaining = 24 - 1 - sortedSeats.size
                    repeat(remaining) {
                        mapItems.add(SeatMapItem.Spacer)
                    }
                }

                availableSeatCount = processedSeats.count { !it.IsBooked }
                seatAdapter.updateItems(mapItems)

                showSeatLoading(false)

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Seat availability API failed",
                    e
                )

                showSeatLoading(false)

                Toast.makeText(
                    requireContext(),
                    "Unable to load seats",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    // LOADING


    private fun showSeatLoading(show: Boolean) {

        if (show) {

            binding.seatMapRecyclerView.visibility =
                View.INVISIBLE

        } else {

            binding.seatMapRecyclerView.visibility =
                View.VISIBLE
        }
    }


    // PROCEED


    private fun setupProceedButton() {

        binding.proceedButton.setOnClickListener {

            // 1. Check if user selected exactly the requested number of seats
            if (selectedSeats.size != requestedQuantity) {
                Toast.makeText(
                    requireContext(),
                    "Please select exactly $requestedQuantity seats",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // 2. Return selected seats
            returnSelectedSeats()
        }
    }

    // BOTTOM BAR


    private fun updateBottomBar() {

        val count = selectedSeats.size

        binding.seatCountText.text =
            "$count / $requestedQuantity"

        binding.bottomBar.visibility =
            if (count > 0) {
                View.VISIBLE
            } else {
                View.GONE
            }
    }


    // RETURN RESULT


    private fun returnSelectedSeats() {

        // NEW: resolve numeric seat IDs from the selected labels
        val selectedSeatIds = selectedSeats.mapNotNull { label -> seatIdMap[label] }

        setFragmentResult(
            REQUEST_KEY,
            bundleOf(
                EXTRA_SELECTED_SEATS to ArrayList(selectedSeats),
                EXTRA_SELECTED_SEAT_IDS to ArrayList(selectedSeatIds),   // NEW
                EXTRA_SCHEDULE_ID to scheduleId
            )
        )

        dismiss()
    }




    private fun dpToPx(dp: Int): Int {

        return (
                dp * resources.displayMetrics.density
                ).toInt()
    }



    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }


    // COMPANION


    companion object {

        private const val TAG = "CustomTicketAllocation"

        const val REQUEST_KEY =
            "seat_allocation_request"

        const val EXTRA_TICKET_ID =
            "extra_ticket_id"

        const val EXTRA_SCHEDULE_ID =
            "extra_schedule_id"

        const val EXTRA_SCREEN_ID =
            "extra_screen_id"

        const val EXTRA_SCREEN_NAME =
            "extra_screen_name"

        const val EXTRA_AVAILABLE_SEATS =
            "extra_available_seats"

        const val EXTRA_SHOW_DAY =
            "extra_show_day"

        const val EXTRA_START_TIME =
            "extra_start_time"
        const val EXTRA_SHOW_DATE =
            "extra_show_date"

        const val EXTRA_PRICE_PER_SEAT =
            "extra_price_per_seat"

        const val EXTRA_SELECTED_SEATS =
            "extra_selected_seats"

        const val EXTRA_SELECTED_SEAT_IDS =
            "extra_selected_seat_ids"

        const val EXTRA_REQUESTED_QUANTITY =
            "extra_requested_quantity"

        const val EXTRA_CANCELLED =
            "extra_cancelled"

        const val EXTRA_INITIAL_SELECTED_SEATS =
            "extra_initial_selected_seats"

        fun newInstance(
            ticketId: Int,
            scheduleId: Int,
            screenId: Int,
            screenName: String,
            availableSeats: Int,
            requestedQuantity: Int,
            showDay: String,
            startTime: String,
            showDate: String,
            pricePerSeat: Double,
            initialSelectedSeats: String? = null
        ): CustomTicketAllocationpopupDialogue {

            return CustomTicketAllocationpopupDialogue().apply {

                arguments = bundleOf(

                    EXTRA_TICKET_ID to
                            ticketId,

                    EXTRA_SCHEDULE_ID to
                            scheduleId,

                    EXTRA_SCREEN_ID to
                            screenId,

                    EXTRA_SCREEN_NAME to
                            screenName,

                    EXTRA_AVAILABLE_SEATS to
                            availableSeats,

                    EXTRA_REQUESTED_QUANTITY to
                            requestedQuantity,

                    EXTRA_SHOW_DAY to
                            showDay,

                    EXTRA_START_TIME to
                            startTime,
                    EXTRA_SHOW_DATE to
                            showDate,

                    EXTRA_PRICE_PER_SEAT to
                            pricePerSeat,

                    EXTRA_INITIAL_SELECTED_SEATS to
                            initialSelectedSeats
                )
            }
        }
    }
}


// SEAT ADAPTER


class SeatAdapter(
    private var items: List<SeatMapItem>,
    private val selectedSeats: MutableSet<String>,
    private val requestedQuantity: Int,
    private val onSelectionChanged: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_LABEL = 0
        private const val TYPE_SEAT = 1
        private const val TYPE_SPACER = 2
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is SeatMapItem.RowLabel -> TYPE_LABEL
            is SeatMapItem.SeatItem -> TYPE_SEAT
            is SeatMapItem.Spacer -> TYPE_SPACER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_LABEL -> {
                val view = inflater.inflate(R.layout.item_row_label, parent, false)
                LabelViewHolder(view)
            }
            TYPE_SEAT -> {
                val binding = ItemSeatBinding.inflate(inflater, parent, false)
                SeatViewHolder(binding)
            }
            else -> {
                val view = View(parent.context).apply {
                    layoutParams = ViewGroup.LayoutParams(42, 42) // Match seat size
                }
                SpacerViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        if (holder is LabelViewHolder && item is SeatMapItem.RowLabel) {
            holder.textView.text = item.label
        } else if (holder is SeatViewHolder && item is SeatMapItem.SeatItem) {
            bindSeat(holder, item.seat)
        }
    }

    private fun bindSeat(holder: SeatViewHolder, seat: SeatAvailability) {
        val seatId = "${seat.RowName}-${seat.SeatNumber}"
        holder.binding.seatNumber.text = seat.SeatNumber.toString()

        when {
            seat.IsBooked -> {
                applyState(holder, SeatState.BOOKED)
                holder.binding.root.setOnClickListener(null)
            }
            selectedSeats.contains(seatId) -> {
                applyState(holder, SeatState.SELECTED)
                holder.binding.root.setOnClickListener {
                    selectedSeats.remove(seatId)
                    notifyItemChanged(holder.bindingAdapterPosition)
                    onSelectionChanged()
                }
            }
            else -> {
                applyState(holder, SeatState.AVAILABLE)
                holder.binding.root.setOnClickListener {
                    if (selectedSeats.size >= requestedQuantity) {
                        Toast.makeText(
                            holder.binding.root.context,
                            "You can only select $requestedQuantity seats",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnClickListener
                    }
                    selectedSeats.add(seatId)
                    notifyItemChanged(holder.bindingAdapterPosition)
                    onSelectionChanged()
                }
            }
        }
    }

    private fun applyState(holder: SeatViewHolder, state: SeatState) {
        val ctx = holder.binding.root.context
        val (bgRes, tintColor, textColor) = when (state) {
            SeatState.AVAILABLE -> Triple(
                R.drawable.bg_seat_available,
                ctx.getColor(R.color.forest),
                ctx.getColor(R.color.forest)
            )
            SeatState.SELECTED -> Triple(
                R.drawable.bg_seat_selected,
                ctx.getColor(R.color.white),
                ctx.getColor(R.color.white)
            )
            SeatState.BOOKED -> Triple(
                R.drawable.bg_seat_booked,
                ctx.getColor(R.color.white),
                ctx.getColor(R.color.white)
            )
        }

        holder.binding.seatBox.setBackgroundResource(bgRes)
        holder.binding.imgSeatIcon.imageTintList = android.content.res.ColorStateList.valueOf(tintColor)
        holder.binding.seatNumber.setTextColor(textColor)
    }

    fun updateItems(newItems: List<SeatMapItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size

    class LabelViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: android.widget.TextView = view.findViewById(R.id.txtRowLabel)
    }

    class SeatViewHolder(val binding: ItemSeatBinding) : RecyclerView.ViewHolder(binding.root)
    class SpacerViewHolder(view: View) : RecyclerView.ViewHolder(view)
}





class SeatSpacingDecoration(
    private val horizontalSpace: Int,
    private val verticalSpace: Int,
    private val spanCount: Int,
    private val gapAfterRows: Int
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {

        val position =
            parent.getChildAdapterPosition(view)

        if (position == RecyclerView.NO_POSITION) {
            return
        }

        val column =
            position % spanCount

        val row =
            position / spanCount




        if (column > 0) {
            outRect.left =
                horizontalSpace / 2
        }

        if (column < spanCount - 1) {
            outRect.right =
                horizontalSpace / 2
        }


        if (row > 0) {
            outRect.top =
                verticalSpace / 2
        }

        if (row < getLastRow(parent)) {
            outRect.bottom =
                verticalSpace / 2
        }



        if (
            row > 0 &&
            row % gapAfterRows == 0
        ) {

            outRect.top +=
                verticalSpace
        }
    }

    private fun getLastRow(
        parent: RecyclerView
    ): Int {

        val itemCount =
            parent.adapter?.itemCount ?: 0

        if (itemCount == 0) {
            return 0
        }

        return (itemCount - 1) / spanCount
    }
}