package com.xenia.ticket.ui.dialog


import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Base64
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.xenia.ticket.R
import com.xenia.ticket.data.network.model.TicketPaymentRequest
import com.xenia.ticket.data.repository.CompanyRepository
import com.xenia.ticket.data.repository.PaymentRepository
import com.xenia.ticket.data.repository.TicketRepository
import com.xenia.ticket.ui.sreens.kiosk.LanguageActivity
import com.xenia.ticket.ui.sreens.kiosk.PaymentActivity
import com.xenia.ticket.utils.common.ApiResponseHandler
import com.xenia.ticket.utils.common.CommonMethod.generateNumericTransactionReferenceID
import com.xenia.ticket.utils.common.CommonMethod.showSnackbar
import com.xenia.ticket.utils.common.CompanyKey
import com.xenia.ticket.utils.common.SessionManager
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.xenia.ticket.utils.common.JwtUtils
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject
import retrofit2.HttpException
import java.util.Locale

class CustomQRDarshanPopupDialogue : DialogFragment() {

    private lateinit var timerTextView: TextView
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


    fun setData(
        amount: String,
        url: String,
        transactionReferenceID: String,
        name: String,
        phoneNumber : String,

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
        qrCodeImageView = view.findViewById(R.id.qrCodeImageView)
        timerTextView = view.findViewById(R.id.txt_timer)
        imgLogo = view.findViewById(R.id.imgLogo)
        viewLifecycleOwner.lifecycleScope.launch {


            val gateway = companyRepository.getString(CompanyKey.PAYMENT_GATEWAY)
            gateway?.let {
                when (gateway) {
                    "CanaraBank" -> {
                        imgLogo.setImageDrawable(
                            ContextCompat.getDrawable(
                                requireContext(),
                                R.drawable.ic_cann
                            )
                        )
                    }
                    "FederalBank" -> {
                        imgLogo.setImageDrawable(
                            ContextCompat.getDrawable(
                                requireContext(),
                                R.drawable.ic_fed
                            )
                        )
                    }
                    else -> {
                        imgLogo.setImageDrawable(
                            ContextCompat.getDrawable(
                                requireContext(),
                                R.drawable.ic_sib
                            )
                        )
                    }
                }
            } ?: run {
                imgLogo.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_sib
                    )
                )
            }
            val amountValue: Float = amount.toFloat()
            val formattedAmount = String.format(Locale.ENGLISH, "%.2f", amountValue)
            amountTextView.text = getString(R.string.amount) + " Rs. $formattedAmount /-"
            val qrCodeBitmap = generateUPIQRCode(url)
            qrCodeImageView.setImageBitmap(qrCodeBitmap)

            startTimer()


            view.findViewById<ImageView>(R.id.btnClose).setOnClickListener {
                dismiss()
            }

        }
    }

    private var companyGateway: String? = null

    private fun startTimer() {
        val totalTime = 300_000L
        var elapsedTime = 0L
        val pollInterval = 3_000L

        viewLifecycleOwner.lifecycleScope.launch {
            companyGateway = companyRepository.getString(CompanyKey.PAYMENT_GATEWAY)
        }

        pollingTimer = object : CountDownTimer(totalTime, 1000) {
            @SuppressLint("SetTextI18n", "DefaultLocale")
            override fun onTick(millisUntilFinished: Long) {
                elapsedTime += 1000
                val minutes = millisUntilFinished / 60000
                val seconds = (millisUntilFinished % 60000) / 1000
                val timeFormatted = String.format("%02d:%02d", minutes, seconds)
                timerTextView.text = getString(R.string.qr_expire) + " " + timeFormatted

                if (elapsedTime % pollInterval == 0L) {
                    when (companyGateway) {
                        "FederalBank" -> checkFedPaymentStatus()
                        "CanaraBank" -> {

                        }
//                        else -> checkPaymentStatus()
                    }
                }
            }

            @SuppressLint("SetTextI18n")
            override fun onFinish() {
                stopCheckingPaymentStatus()
                val intent = Intent(requireContext(), LanguageActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                dismiss()
            }
        }.start()
    }


    private fun checkFedPaymentStatus() {
        if (isCheckingPaymentStatus) return

        isCheckingPaymentStatus = true

        paymentStatusJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                while (isCheckingPaymentStatus) {

                    val response = withContext(Dispatchers.IO) {
                        paymentRepository.getFedPaymentStatus(
                            orderId = transactionReferenceID,
                            token = sessionManager.getToken().toString()
                        )
                    }

                    when (response.status) {
                        "SUCCESS" -> {
                            postTicketPaymentHistory( "S","Payment Success")
                            break
                        }

                        "FAILED" -> {
                            postTicketPaymentHistory("F", "Transaction failed")
                            break
                        }

                        "PENDING" -> {

                        }
                        "PENDING" -> {
                            // ✅ Keep polling until timer ends
                        }

                        else -> {
                            stopCheckingPaymentStatus()
                            break
                        }
                    }

                    delay(2000)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isCheckingPaymentStatus = false
            }
        }
    }

    private suspend fun postTicketPaymentHistory(
        status: String,
        statusDesc: String,
        retryCount: Int = 0
    ) {
        try {
            val cartTickets = ticketRepository.getAllTicketsInCart()
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
                firstTicket.daImg ?: ByteArray(0),
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

            Log.d("PAYMENT_DEBUG", "Posting ${itemsList.size} items")

            ApiResponseHandler.handleApiCall(
                activity = requireActivity(),
                apiCall = {
                    withContext(Dispatchers.IO) {
                        paymentRepository.postTicket(
                            bearerToken = "Bearer $token",
                            request = request
                        )
                    }
                },
                onSuccess = { response ->
                    if (response.status.equals("Success", ignoreCase = true)
                        && !response.receipt.isNullOrBlank()
                    ) {
                        lifecycleScope.launch {
                            val (totalAmount) = ticketRepository.getCartStatus()

                            handleTicketTransactionStatus(
                                status = "S",
                                orderId = response.receipt,
                                ticket = response.ticket,
                                totalAmount = totalAmount.toDouble(),
                                receiptPrefix = companyRepository.getString(CompanyKey.PREFIX) ?: ""
                            )
                        }
                    } else {
                        showSnackbar(requireView(), "Failed to post order")
                    }
                }
            )
        } catch (e: HttpException) {
            withContext(Dispatchers.Main) {
                if (e.code() == 401) {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Logout !!")
                        .setMessage(
                            "You have been logged out because your account was used on another device."
                        )
                        .setCancelable(false)
                        .setPositiveButton("Logout") { _, _ ->
                            ApiResponseHandler.logoutUser(requireActivity())
                        }
                        .show()
                } else {
                    handleRetry(e, status, statusDesc, retryCount)
                }
            }


        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                showSnackbar(requireView(), "Something went wrong")
            }
        }
    }
    private fun handleRetry(
        e: Exception,
        status: String,
        statusDesc: String,
        retryCount: Int
    ) {
        if (e is HttpException && e.code() == 401) {
            showSnackbar(requireView(),  "Unauthorized: Please login again")

            return
        }

        if (retryCount < 3) {
            lifecycleScope.launch {
                postTicketPaymentHistory(status, statusDesc, retryCount + 1)
            }

        } else {
            showSnackbar(requireView(), "Failed: ${e.message}")
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
        dismiss()


        startActivity(intent)
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
