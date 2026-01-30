package com.xenia.ticket.ui.sreens.billing

import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.xenia.ticket.R
import com.xenia.ticket.data.network.model.SummaryReportResponse
import com.xenia.ticket.data.repository.ReportRepository
import com.xenia.ticket.databinding.ActivitySummaryReportBinding
import com.xenia.ticket.ui.dialog.CustomInternetAvailabilityDialog
import com.xenia.ticket.ui.sreens.kiosk.PrinterSettingActivity
import com.xenia.ticket.utils.common.CommonMethod.dismissLoader
import com.xenia.ticket.utils.common.CommonMethod.isInternetAvailable
import com.xenia.ticket.utils.common.CommonMethod.setLocale
import com.xenia.ticket.utils.common.CommonMethod.showLoader
import com.xenia.ticket.utils.common.CommonMethod.showSnackbar
import com.xenia.ticket.utils.common.JwtUtils
import com.xenia.ticket.utils.common.ReportPrint
import com.xenia.ticket.utils.common.SessionManager
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.xenia.ticket.utils.common.ApiResponseHandler
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.getValue

class SummaryReportActivity : AppCompatActivity(),
    CustomInternetAvailabilityDialog.InternetAvailabilityListener{

    private lateinit var binding: ActivitySummaryReportBinding
    private val reportRepository: ReportRepository by inject()
    private val sessionManager: SessionManager by inject()
    private val customInternetAvailabilityDialog: CustomInternetAvailabilityDialog by inject()
    private val reportPrint: ReportPrint by inject()
    private var lastReportData: SummaryReportResponse? = null
    private var reportedTime: String? = null
    private var selectedLanguage: String? = null
    private lateinit var jwtToken: String
    private var userId: Int = 0
    companion object {
        private const val UI_DATE_FORMAT = "dd/MM/yyyy hh:mm a"
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySummaryReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        selectedLanguage = sessionManager.getBillingSelectedLanguage()
        setLocale(this, selectedLanguage)
        jwtToken = sessionManager.getToken() ?: throw IllegalStateException("Token missing")
        userId = JwtUtils.getUserId(jwtToken) ?: throw IllegalStateException("UserId missing in JWT")

        initUI()

        val calendar = Calendar.getInstance()
        val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.ENGLISH)

        val startCalendar = calendar.clone() as Calendar
        startCalendar.set(Calendar.HOUR_OF_DAY, 0)
        startCalendar.set(Calendar.MINUTE, 0)
        startCalendar.set(Calendar.SECOND, 0)
        startCalendar.set(Calendar.MILLISECOND, 0)
        val startDateTime = dateTimeFormat.format(startCalendar.time)

        val endDateTime = dateTimeFormat.format(calendar.time)

        reportedTime = getString(R.string.report_generated_time)
        binding.txtGeneratedTime.text = "$reportedTime : $endDateTime"
        binding.txtFromDateTime.setText(startDateTime)
        binding.txtToDateTime.setText(endDateTime)
        Log.d("SUMMARY_API_AUTH", "Token = $jwtToken")

        callSummaryReportApi(startDateTime, endDateTime)

        binding.txtFromDateTime.setOnClickListener {
            showMaterialDateTimePicker(binding.txtFromDateTime) { newFromDate ->

                val newFrom = parseUiDate(newFromDate)
                val currentTo = parseUiDate(binding.txtToDateTime.text.toString())

                if (newFrom != null && currentTo != null && newFrom.after(currentTo)) {
                    binding.txtToDateTime.setText(newFromDate)
                }

                callSummaryReportApi(
                    newFromDate,
                    binding.txtToDateTime.text.toString()
                )
            }
        }


        binding.txtToDateTime.setOnClickListener {
            showMaterialDateTimePicker(binding.txtToDateTime) { newToDate ->

                val newTo = parseUiDate(newToDate)
                val currentFrom = parseUiDate(binding.txtFromDateTime.text.toString())

                if (newTo != null && currentFrom != null && newTo.before(currentFrom)) {
                    binding.txtFromDateTime.setText(newToDate)
                }

                callSummaryReportApi(
                    binding.txtFromDateTime.text.toString(),
                    newToDate
                )
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
            val userName = JwtUtils.getUsername(jwtToken)
            lastReportData?.let { data ->
                reportPrint.printDailySummary(
                    reportStart = binding.txtFromDateTime.text.toString(),
                    reportEnd = binding.txtToDateTime.text.toString(),
                    cash = data.TotalCash.toDouble(),
                    card = data.TotalCard.toDouble(),
                    upi = data.TotalUpi.toDouble(),
                    donation = data.TotalOrderAmount.toDouble(),
                    Seva_Particulars = data.TotalVazhipaduOfferingsAmount.toDouble(),
                    pooja_items = data.TotalPoojaItemAmount.toDouble(),
                    darshan = data.TotalDarshanAmount.toDouble(),
                    net = data.TotalAmount.toDouble(),
                    generatedBy = userName.toString(),
                    selectedLanguage = selectedLanguage!!
                )

            }
            Log.d("SummaryReport", "Printing completed call")

        }

    }
    private fun initUI() {
        reportedTime = getString(R.string.report_generated_time)
        binding.txtTotalAmount.text = getString(R.string.total_amount)
        binding.txtReport.text = getString(R.string.report)
        binding.txtSummary.text = getString(R.string.summary)
        binding.txtTransaction.text = getString(R.string.transaction)
        binding.ticket.text = getString(R.string.ticket)


    }
    @SuppressLint("SetTextI18n", "DefaultLocale")
    private fun callSummaryReportApi(startDate: String, endDate: String) {

        if (!isInternetAvailable(this)) {
            dismissLoader()
            showSnackbar(binding.root, getString(R.string.no_internet_connection))
            if (!customInternetAvailabilityDialog.isAdded && !isFinishing && !isDestroyed) {
                customInternetAvailabilityDialog.show(
                    supportFragmentManager,
                    "internet_availability_dialog"
                )
            }
            return
        }

        val formattedStart = formatForApi(startDate)
        val formattedEnd = formatForApi(endDate)

        if (formattedStart == null || formattedEnd == null) {
            showSnackbar(binding.root, "Invalid date format")
            return
        }

        showLoader(this, "Loading summary report...")

        lifecycleScope.launch {
            try {
                val token=sessionManager.getToken()
                val response = reportRepository.getSummaryReport(
                    token = token.toString(),
                    startDateTime = formattedStart,
                    endDateTime = formattedEnd
                )

                binding.txtTotalDarshan.text =
                    "Rs.${String.format(Locale.ENGLISH,"%.2f", response.TotalDarshanAmount.toDouble())}/-"

                binding.txtCash.text =
                    "Cash\nRs.${String.format(Locale.ENGLISH,"%.2f", response.TotalCash.toDouble())}/-"

                binding.txtCard.text =
                    "Card\nRs.${String.format(Locale.ENGLISH,"%.2f", response.TotalCard.toDouble())}/-"

                binding.txtUpi.text =
                    "UPI\nRs.${String.format(Locale.ENGLISH,"%.2f", response.TotalUpi.toDouble())}/-"

                binding.txtTotalAmount.text =
                    "${getString(R.string.total_amount)} : Rs.${
                        String.format(Locale.ENGLISH,"%.2f", response.TotalAmount.toDouble())
                    }/-"


                lastReportData = response

                val isPrintable = response.TotalAmount.toDouble() > 0.0
                binding.btnPrint.isEnabled = isPrintable
                binding.btnPrint.alpha = if (isPrintable) 1.0f else 0.5f

            } catch (e: HttpException) {
                if (e.code() == 401) {
                    AlertDialog.Builder(this@SummaryReportActivity)
                        .setTitle("Logout !!")
                        .setMessage("You have been logged out because your account was used on another device.")
                        .setCancelable(false)
                        .setPositiveButton("Logout") { _, _ ->
                            ApiResponseHandler.logoutUser(this@SummaryReportActivity)
                        }
                        .show()
                } else {
                    throw e
                }
            } catch (e: Exception) {
                showSnackbar(binding.root, "Error loading categories")
            }
            finally {
                dismissLoader()
            }
        }
    }


    fun parseUiDate(input: String): Date? = try {
        val sdf = SimpleDateFormat(UI_DATE_FORMAT, Locale.ENGLISH)
        sdf.isLenient = false
        sdf.parse(input)
    } catch (e: Exception) {
        null
    }

    fun formatForApi(input: String): String? = try {
        val ui = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.ENGLISH)
        val api = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.ENGLISH)
        api.format(ui.parse(input)!!)
    } catch (e: Exception) {
        null
    }
    private fun setEnglishLocale() {
        val locale = Locale("en")
        Locale.setDefault(locale)

        val config = resources.configuration
        config.setLocale(locale)

        resources.updateConfiguration(config, resources.displayMetrics)
    }

    private fun showMaterialDateTimePicker(
        targetEditText: EditText,
        onDateSelected: (String) -> Unit
    ) {
        setEnglishLocale()
        val constraintsBuilder = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointBackward.now())

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Date")
            .setSelection(System.currentTimeMillis())
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

    override fun onRetryClicked() {
        if (isInternetAvailable(this)) {
            supportFragmentManager.findFragmentByTag(
                "internet_availability_dialog"
            )?.let {
                (it as DialogFragment).dismiss()
            }
        }
    }

    override fun onDialogInactive() {
    }
}