    package com.xenia.ticket.ui.dialog

    import android.annotation.SuppressLint
    import android.app.Dialog
    import android.content.DialogInterface
    import android.content.Intent
    import android.graphics.Bitmap
    import android.graphics.Color
    import android.os.Bundle
    import android.os.CountDownTimer
    import android.os.Handler
    import android.os.Looper
    import android.util.Base64
    import android.util.Log
    import android.view.Gravity
    import android.view.LayoutInflater
    import android.view.View
    import android.view.ViewGroup
    import android.view.Window
    import android.widget.Button
    import android.widget.ImageView
    import android.widget.TextView
    import androidx.core.graphics.drawable.toDrawable
    import androidx.fragment.app.DialogFragment
    import androidx.lifecycle.lifecycleScope
    import com.xenia.ticket.R
    import com.xenia.ticket.data.network.model.TicketPaymentRequest
    import com.xenia.ticket.data.repository.CompanyRepository
    import com.xenia.ticket.data.repository.PaymentRepository
    import com.xenia.ticket.data.repository.TicketRepository
    import com.xenia.ticket.ui.screens.kiosk.PaymentActivity
    import com.xenia.ticket.utils.common.ApiResponseHandler
    import com.xenia.ticket.utils.common.CompanyKey
    import com.xenia.ticket.utils.common.SessionManager
    import com.google.zxing.BarcodeFormat
    import com.google.zxing.MultiFormatWriter
    import com.google.zxing.WriterException
    import com.journeyapps.barcodescanner.BarcodeEncoder
    import com.xenia.ticket.utils.common.CommonMethod.dismissLoader
    import com.xenia.ticket.utils.common.CommonMethod.showLoader
    import com.xenia.ticket.utils.common.JwtUtils
    import kotlinx.coroutines.*
    import org.koin.android.ext.android.inject
    import java.util.Locale
    import com.xenia.ticket.data.network.model.PaymentStatusResponse
    import com.xenia.ticket.data.network.model.SibPaymentStatusResponse
    import kotlinx.coroutines.sync.Mutex

    class CustomQRDarshanPopupDialogue : DialogFragment() {

        private lateinit var timerTextView: TextView
        private lateinit var textTranscation: TextView
        private lateinit var amountTextView: TextView
        private lateinit var qrCodeImageView: ImageView

        private var amount: String = ""
        private var url: String = ""
        private var transactionReferenceID: String = ""
        private var name: String = ""
        private var phoneNumber: String = ""
        private lateinit var imgLogo: ImageView
        private var pollingTimer: CountDownTimer? = null
        private var paymentStatusJob: Job? = null
        private val paymentRepository: PaymentRepository by inject()
        private val sessionManager: SessionManager by inject()
        private val ticketRepository: TicketRepository by inject()
        private val companyRepository: CompanyRepository by inject()
        private var isCheckingPaymentStatus = false
        private lateinit var btnCheckStatus: Button
        private var lastKnownStatus: String = "PENDING"
        private val autoCheckHandler = Handler(Looper.getMainLooper())
        private var autoCheckRunnable: Runnable? = null
        private var countdownValue = 10
        private var paymentGateway: String = ""
        private val paymentMutex = Mutex()
        private var isFinalStatusHandled = false


        fun setData(
            amount: String,
            url: String,
            transactionReferenceID: String,
            name: String,
            phoneNumber: String,

            ) {
            this.amount = amount
            this.url = url
            this.transactionReferenceID = transactionReferenceID
            this.name = name
            this.phoneNumber = phoneNumber
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val dialog = Dialog(requireActivity(), theme)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            dialog.setCanceledOnTouchOutside(false)
            dialog.setCancelable(false)
            dialog.setOnKeyListener { _, keyCode, _ ->
                keyCode == android.view.KeyEvent.KEYCODE_BACK

            }

            return dialog
        }

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            return inflater.inflate(R.layout.custome_qr_dialogue, container, false)
        }

        @SuppressLint("SetTextI18n", "DefaultLocale")
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            amountTextView = view.findViewById(R.id.txt_amount)
            textTranscation = view.findViewById(R.id.textTranscation)
            qrCodeImageView = view.findViewById(R.id.qrCodeImageView)
            timerTextView = view.findViewById(R.id.txt_timer)
            imgLogo = view.findViewById(R.id.imgLogo)
            btnCheckStatus = view.findViewById(R.id.btnCheckStatus)

            viewLifecycleOwner.lifecycleScope.launch {

                paymentGateway = companyRepository.getString(CompanyKey.PAYMENT_GATEWAY) ?: ""

                when (paymentGateway) {
                    "CanaraBank" -> imgLogo.setImageResource(R.drawable.ic_cann)
                    "FederalBank" -> imgLogo.setImageResource(R.drawable.ic_fed)
                    "SouthIndianBank" -> imgLogo.setImageResource(R.drawable.ic_sib)
                    else -> imgLogo.setImageResource(R.drawable.ic_sib)
                }

                val formattedAmount = String.format(Locale.ENGLISH, "%.2f", amount.toFloat())
                amountTextView.text = "Amount Rs. $formattedAmount /-"
                textTranscation.text = "Transaction ID : $transactionReferenceID"

                qrCodeImageView.setImageBitmap(generateUPIQRCode(url))

                startTimer()

                view.findViewById<ImageView>(R.id.btnClose).setOnClickListener {
                   safeDismiss()
                }

                btnCheckStatus.setOnClickListener {
                    btnCheckStatus.isEnabled = false
                    checkPaymentStatus(true)
                }
            }
        }

        override fun onDestroyView() {
            stopAutoCheckCountdown()
            super.onDestroyView()
        }

        private fun startTimer() {
            val totalTime = 300000L
            var elapsedTime = 0L

            pollingTimer = object : CountDownTimer(totalTime, 1000) {
                @SuppressLint("SetTextI18n")
                override fun onTick(millisUntilFinished: Long) {
                    elapsedTime += 1000

                    val min = millisUntilFinished / 60000
                    val sec = (millisUntilFinished % 60000) / 1000
                    timerTextView.text = "QR Expire in %02d:%02d".format(min, sec)

                    if (elapsedTime % 3000 == 0L) {
                        checkPaymentStatus(false)
                    }
                }

                @SuppressLint("SetTextI18n")
                override fun onFinish() {
                    if (lastKnownStatus == "PENDING" || lastKnownStatus == "RETRY") {
                        timerTextView.text = "Payment pending!"
                        btnCheckStatus.visibility = View.VISIBLE
                        startAutoCheckCountdown()
                    } else dismiss()
                }
            }.start()
        }

        private fun startAutoCheckCountdown() {
            countdownValue = 10

            autoCheckRunnable = object : Runnable {
                @SuppressLint("SetTextI18n")
                override fun run() {
                    btnCheckStatus.text = "Check Status ($countdownValue)"

                    if (countdownValue == 0) {
                        countdownValue = 10
                        checkPaymentStatus(true)
                    } else countdownValue--

                    autoCheckHandler.postDelayed(this, 1000)
                }
            }
            autoCheckHandler.post(autoCheckRunnable!!)
        }


        @SuppressLint("SetTextI18n")
        private fun stopAutoCheckCountdown() {
            pollingTimer?.cancel()
            paymentStatusJob?.cancel()
            autoCheckRunnable?.let {
                autoCheckHandler.removeCallbacks(it)
            }
            btnCheckStatus.text = "Check Status"
        }


        @SuppressLint("SetTextI18n")
        private fun checkPaymentStatus(isManualCheck: Boolean) {
            paymentStatusJob = lifecycleScope.launch {
                if (!paymentMutex.tryLock()) {
                    Log.d("PAYMENT_FLOW", "Skipped - another request running")
                    return@launch
                }
                try {

                    if (isManualCheck && isAdded) {
                        showLoader(requireContext(), "Checking status...")
                    }

                    val response = withContext(Dispatchers.IO) {

                        when (paymentGateway) {

                            "FederalBank" -> paymentRepository.getFedPaymentStatus(
                                transactionReferenceID,
                                sessionManager.getToken().toString()
                            )

                            else -> paymentRepository.getSibPaymentStatus(
                                transactionReferenceID,
                                sessionManager.getToken().toString()
                            )
                        }
                    }

                    val (rawStatus, rawDesc) = when (response) {
                        is PaymentStatusResponse -> Pair(response.status, "")
                        is SibPaymentStatusResponse -> Pair(response.Status, response.statusDesc)
                        else -> Pair("PENDING", null)
                    }

                    val finalStatus = normalizeStatus(rawStatus, rawDesc)

                    Log.d("PAYMENT_FLOW", "Final Status Triggered: $finalStatus")

                    lastKnownStatus = finalStatus

                    if (isFinalStatusHandled) return@launch

                    when (finalStatus) {

                        "SUCCESS" -> {
                            isFinalStatusHandled = true

                            pollingTimer?.cancel()
                            stopAutoCheckCountdown()

                            Log.d("PAYMENT_FLOW", "Calling SUCCESS API")

                            withContext(NonCancellable) {
                                postTicketPaymentHistory("S", "Payment Success")
                            }

                            return@launch
                        }

                        "FAILED" -> {
                            isFinalStatusHandled = true

                            pollingTimer?.cancel()
                            stopAutoCheckCountdown()

                            Log.d("PAYMENT_FLOW", "Calling FAILED API")

                            withContext(NonCancellable) {
                                postTicketPaymentHistory("F", rawDesc ?: "Transaction Failed")
                            }

                            return@launch
                        }
                    }

                } catch (e: Exception) {

                    Log.e("PAYMENT_ERROR", "Error", e)

                    if (isAdded) {
                        timerTextView.text = "Still pending..."
                    }

                } finally {

                    if (isManualCheck && isAdded) {
                        btnCheckStatus.isEnabled = true
                        dismissLoader()
                    }

                    if (paymentMutex.isLocked) {
                        paymentMutex.unlock()
                    }
                }
            }
        }

        private fun normalizeStatus(status: String?, desc: String?): String {

            val s = status?.trim()?.uppercase()
            val d = desc?.trim()?.uppercase()

            return when {

                s in listOf("S", "SUCCESS", "00") ||
                        d?.contains("SUCCESS") == true -> "SUCCESS"

                s == "P" ||
                        d?.contains("INITIATED") == true ||
                        d?.contains("PENDING") == true ||
                        (s == "F" && d?.contains("INVALID") == true) -> "PENDING"

                s == "F" ||
                        d?.contains("FAILED") == true -> "FAILED"

                else -> "RETRY"
            }
        }


        private fun safeDismiss() {
            if (isAdded && !isStateSaved) dismiss()
        }

        private suspend fun postTicketPaymentHistory(
            status: String,
            statusDesc: String
        ) {
            try {

                Log.d("PAYMENT_FLOW", "Entered postTicketPaymentHistory")

                val cartTickets = ticketRepository.getAllTicketsInCart()

                if (cartTickets.isEmpty()) {
                    handleTicketTransactionStatus("F", transactionReferenceID, null, 0.0, "")
                    return
                }

                val itemsList = cartTickets.flatMap { item ->
                    (1..item.daQty).map {
                        TicketPaymentRequest.Item(
                            taCategoryId = item.ticketCategoryId,
                            TicketId = item.ticketId,
                            Quantity = 1,
                            Rate = item.daRate
                        )
                    }
                }

                val firstTicket = cartTickets.first()

                val imageBase64String = Base64.encodeToString(
                    firstTicket.daImg,
                    Base64.NO_WRAP
                )

                val token = sessionManager.getToken().toString()
                val companyId = JwtUtils.getCompanyId(token)

                val request = TicketPaymentRequest(
                    CompanyId = companyId!!,
                    UserId = sessionManager.getUserId(),
                    Name = name,
                    tTranscationId = transactionReferenceID,
                    tCustRefNo = "",
                    tNpciTransId = "",
                    tIdProofNo = "",
                    tImage = imageBase64String,
                    PhoneNumber = phoneNumber,
                    tPaymentStatus = status,
                    tPaymentMode = "UPI",
                    tPaymentDes = statusDesc,
                    Items = itemsList
                )

                Log.d("PAYMENT_FLOW", "Calling postTicket API")

                // ✅ CRITICAL FIX
                val response = withContext(NonCancellable) {
                    ApiResponseHandler.handleApiCall(
                        activity = requireActivity()
                    ) {
                        paymentRepository.postTicket(
                            bearerToken = "Bearer $token",
                            request = request
                        )
                    }
                }

                val (totalAmount) = ticketRepository.getCartStatus()

                if (response != null) {

                    handleTicketTransactionStatus(
                        "S",
                        response.receipt ?: transactionReferenceID,
                        response.ticket,
                        totalAmount,
                        companyRepository.getString(CompanyKey.PREFIX) ?: ""
                    )

                } else {

                    handleTicketTransactionStatus(
                        "F",
                        transactionReferenceID,
                        null,
                        totalAmount,
                        companyRepository.getString(CompanyKey.PREFIX) ?: ""
                    )
                }

            } catch (e: Exception) {

                Log.e("PAYMENT_FLOW", "FINAL ERROR", e)

                val (totalAmount) = ticketRepository.getCartStatus()

                handleTicketTransactionStatus(
                    "F",
                    transactionReferenceID,
                    null,
                    totalAmount,
                    ""
                )
            }
        }


        private fun handleTicketTransactionStatus(
            status: String,
            orderId: String,
            ticket: String?,
            totalAmount: Double,
            receiptPrefix: String?
        ) {
            val intent = Intent(requireContext(), PaymentActivity::class.java).apply {
                putExtra("status", status)
                putExtra("amount", totalAmount.toString())
                putExtra("orderID", orderId)
                putExtra("ticket", ticket)
                putExtra("prefix", receiptPrefix)
                putExtra("name", name)
                putExtra("phno", phoneNumber)
                putExtra("transID", transactionReferenceID)
            }
            startActivity(intent)
            lifecycleScope.launch {
                delay(300)
                safeDismiss()
            }
        }

        private fun generateUPIQRCode(url: String): Bitmap? {
            return try {
                val bitMatrix = MultiFormatWriter().encode(
                    url, BarcodeFormat.QR_CODE, 300, 300
                )
                val barcodeEncoder = BarcodeEncoder()
                barcodeEncoder.createBitmap(bitMatrix)
            } catch (e: WriterException) {
                e.printStackTrace()
                null
            }
        }

        override fun onStart() {
            super.onStart()
            dialog?.window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.75).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            dialog?.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            dialog?.window?.setGravity(Gravity.CENTER)
        }

        override fun onDismiss(dialog: DialogInterface) {
            super.onDismiss(dialog)
            pollingTimer?.cancel()
            stopCheckingPaymentStatus()
        }

        private fun stopCheckingPaymentStatus() {
            isCheckingPaymentStatus = false
            paymentStatusJob?.cancel()
        }
    }
