package com.example.ticket.ui.sreens.billing

import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ticket.R
import com.example.ticket.data.network.model.ItemSummaryReportResponse
import com.example.ticket.data.network.model.OfferItem
import com.example.ticket.data.repository.ReportRepository
import com.example.ticket.databinding.ActivityDetailedReportBinding
import com.example.ticket.ui.adapter.DisplayItem
import com.example.ticket.ui.adapter.ItemSummaryAdapter
import com.example.ticket.ui.dialog.CustomInternetAvailabilityDialog
import com.example.ticket.ui.sreens.screen.PrinterSettingActivity
import com.example.ticket.utils.common.CommonMethod.dismissLoader
import com.example.ticket.utils.common.CommonMethod.isInternetAvailable
import com.example.ticket.utils.common.CommonMethod.setLocale
import com.example.ticket.utils.common.CommonMethod.showLoader
import com.example.ticket.utils.common.CommonMethod.showSnackbar
import com.example.ticket.utils.common.JwtUtils
import com.example.ticket.utils.common.ReportPrint
import com.example.ticket.utils.common.SessionManager
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.collections.isNotEmpty


class DetailedReportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailedReportBinding
    private val sessionManager: SessionManager by inject()
    private val reportRepository: ReportRepository by inject()
    private val reportPrint: ReportPrint by inject()
    private var lastReportData: ItemSummaryReportResponse? = null
    private var selectedLanguage: String? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailedReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        selectedLanguage = sessionManager.getBillingSelectedLanguage()
        setLocale(this, selectedLanguage)

        binding.tvTotalLabel.text = getString(R.string.total_amount)


        binding.spinnerRange.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selection = parent.getItemAtPosition(position).toString()
                updateDateRange(selection)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        binding.spinnerRange.setSelection(0)

        binding.edtFromDate.setOnClickListener {
            if (binding.spinnerRange.selectedItem.toString() == "Custom") {
                showMaterialDateTimePicker(binding.edtFromDate) { newDate ->
                    binding.edtFromDate.setText(newDate)
                    callItemSummaryReportApi(newDate, binding.edtToDate.text.toString())
                }
            }
        }

        binding.edtToDate.setOnClickListener {
            if (binding.spinnerRange.selectedItem.toString() == "Custom") {
                showMaterialDateTimePicker(binding.edtToDate) { newDate ->
                    binding.edtToDate.setText(newDate)
                    callItemSummaryReportApi(binding.edtFromDate.text.toString(), newDate)
                }
            }
        }

        reportPrint.setLifecycleScope(lifecycleScope)
        binding.btnPrint.setOnClickListener {
            val selectedPrinter = sessionManager.getSelectedPrinter()
            if (selectedPrinter.isNullOrEmpty()) {
                val printIntent = Intent(this, PrinterSettingActivity::class.java).apply {
                    putExtra("SR", "RS")
                }
                startActivity(printIntent)
                finish()
                return@setOnClickListener
            }

            lastReportData?.let { report ->
                val token=sessionManager.getToken().toString()
                reportPrint.printItemSummaryReport(
                    response = report,
                    fromDate = binding.edtFromDate.text.toString(),
                    toDate = binding.edtToDate.text.toString(),
                    generatedBy = JwtUtils.getUsername(token).toString(),
                    selectedLanguage = selectedLanguage!!
                )
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun updateDateRange(selection: String) {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.ENGLISH)
        val fromCalendar = calendar.clone() as Calendar
        val toCalendar = calendar.clone() as Calendar

        when (selection) {
            "Today" -> {
                fromCalendar.set(Calendar.HOUR_OF_DAY, 0)
                fromCalendar.set(Calendar.MINUTE, 0)
                toCalendar.time = calendar.time
            }
            "Yesterday" -> {
                fromCalendar.add(Calendar.DAY_OF_MONTH, -1)
                fromCalendar.set(Calendar.HOUR_OF_DAY, 0)
                fromCalendar.set(Calendar.MINUTE, 0)
                toCalendar.add(Calendar.DAY_OF_MONTH, -1)
                toCalendar.set(Calendar.HOUR_OF_DAY, 23)
                toCalendar.set(Calendar.MINUTE, 59)
            }
            "This week" -> {
                fromCalendar.set(Calendar.DAY_OF_WEEK, fromCalendar.firstDayOfWeek)
                fromCalendar.set(Calendar.HOUR_OF_DAY, 0)
                fromCalendar.set(Calendar.MINUTE, 0)
                toCalendar.time = calendar.time
            }
            "This month" -> {
                fromCalendar.set(Calendar.DAY_OF_MONTH, 1)
                fromCalendar.set(Calendar.HOUR_OF_DAY, 0)
                fromCalendar.set(Calendar.MINUTE, 0)
                toCalendar.time = calendar.time
            }
            "This quarter" -> {
                val month = calendar.get(Calendar.MONTH)
                val quarterStartMonth = (month / 3) * 3
                fromCalendar.set(Calendar.MONTH, quarterStartMonth)
                fromCalendar.set(Calendar.DAY_OF_MONTH, 1)
                fromCalendar.set(Calendar.HOUR_OF_DAY, 0)
                fromCalendar.set(Calendar.MINUTE, 0)
                toCalendar.time = calendar.time
            }
            "Custom" -> return
        }

        val fromDate = dateFormat.format(fromCalendar.time)
        val toDate = dateFormat.format(toCalendar.time)
        binding.edtFromDate.setText(fromDate)
        binding.edtToDate.setText(toDate)

        callItemSummaryReportApi(fromDate, toDate)
    }

    private fun callItemSummaryReportApi(startDate: String, endDate: String) {
        lifecycleScope.launch {
            try {
                val formattedStart = formatForApi(startDate)
                val formattedEnd = formatForApi(endDate)

                val response = reportRepository.getItemSummaryReport(
                    token  = sessionManager.getToken().toString(),
                    startDateTime  = formattedStart,
                    endDateTime  = formattedEnd,
                )

                dismissLoader()

                if (response.status == "success") {
                    lastReportData = response
                    val displayList = prepareDisplayList(response)

                    if (displayList.isNotEmpty()) {
                        binding.rvItems.layoutManager = LinearLayoutManager(this@DetailedReportActivity)
                        binding.rvItems.adapter = ItemSummaryAdapter(displayList)

                        val totalAmtValue = response.summary.GrandTotalAmountAll
                        binding.tvTotalAmount.text = String.format("%.2f", totalAmtValue)

                        val isPrintable = totalAmtValue > 0.0
                        binding.btnPrint.isEnabled = isPrintable
                        binding.btnPrint.alpha = if (isPrintable) 1.0f else 0.5f
                    } else {
                        binding.rvItems.adapter = ItemSummaryAdapter(emptyList())
                        binding.tvTotalAmount.text = "0.00"

                        binding.btnPrint.isEnabled = false
                        binding.btnPrint.alpha = 0.5f

                        lastReportData = null

                        Toast.makeText(this@DetailedReportActivity, "No items to show", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@DetailedReportActivity, "No data found", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                dismissLoader()
                e.printStackTrace()
                Toast.makeText(this@DetailedReportActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun prepareDisplayList(response: ItemSummaryReportResponse): List<DisplayItem> {
        val displayList = mutableListOf<DisplayItem>()

        fun getLocalizedName(item: OfferItem): String {

            val base = when (selectedLanguage?.lowercase()?.take(2)) {
                "ml" -> item.offerNameMa
                "ta" -> item.offerNameTa
                "te" -> item.offerNameTe
                "hi" -> item.offerNameHi
                "kn" -> item.offerNameKn
                else -> item.offerName
            }

            return base ?: item.offerName ?: item.ticketName ?: ""
        }


        fun addCategoryItems(title: String, items: List<OfferItem>, totalAmount: Double) {
            if (items.isNotEmpty()) {
                displayList.add(DisplayItem.Header(title))
                displayList.add(DisplayItem.ColumnHeader)
                items.forEach { item ->
                    displayList.add(DisplayItem.Item(getLocalizedName(item), item.totalQty, item.rate, item.totalAmount))
                }
                displayList.add(DisplayItem.TotalRow(totalAmount))
            }
        }


        addCategoryItems("Tickets", response.darshanTickets, response.summary.darshanTickets.GrandTotalAmount)

        return displayList
    }


    private fun formatForApi(dateTimeStr: String): String {
        val inputFormat = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.ENGLISH)
        val date: Date = inputFormat.parse(dateTimeStr)!!
        val apiFormat = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.ENGLISH)
        return apiFormat.format(date)
    }

    private fun showMaterialDateTimePicker(targetEditText: EditText, onDateSelected: (String) -> Unit) {
        val constraintsBuilder = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointBackward.now())

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Date")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .setCalendarConstraints(constraintsBuilder.build())
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = selection

            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            val timePicker = TimePickerDialog(
                this,
                { _, selectedHour, selectedMinute ->
                    calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
                    calendar.set(Calendar.MINUTE, selectedMinute)

                    val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.ENGLISH)
                    val dateTimeStr = dateTimeFormat.format(calendar.time)
                    targetEditText.setText(dateTimeStr)
                    onDateSelected(dateTimeStr)
                },
                hour,
                minute,
                false
            )
            timePicker.show()
        }

        datePicker.show(supportFragmentManager, "MATERIAL_DATE_PICKER")
    }
}