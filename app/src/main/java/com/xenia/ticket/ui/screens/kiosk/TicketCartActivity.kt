package com.xenia.ticket.ui.screens.kiosk

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.MotionEvent
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.xenia.ticket.R
import com.xenia.ticket.data.listeners.InactivityHandlerActivity
import com.xenia.ticket.data.listeners.OnTicketClickListener
import com.xenia.ticket.data.network.model.FedQrRequest
import com.xenia.ticket.data.network.model.SibQrRequest
import com.xenia.ticket.data.network.model.TicketDto
import com.xenia.ticket.data.network.model.TicketPaymentRequest
import com.xenia.ticket.data.repository.CompanyRepository
import com.xenia.ticket.data.repository.PaymentRepository
import com.xenia.ticket.data.repository.TicketRepository
import com.xenia.ticket.data.room.entity.Ticket
import com.xenia.ticket.databinding.ActivityTicketCartBinding
import com.xenia.ticket.ui.adapter.TicketCartAdapter
import com.xenia.ticket.ui.dialog.CustomInactivityDialog
import com.xenia.ticket.ui.dialog.CustomInternetAvailabilityDialog
import com.xenia.ticket.ui.dialog.CustomQRDarshanPopupDialogue
import com.xenia.ticket.ui.dialog.CustomTicketPopupDialogue
import com.xenia.ticket.utils.common.ApiResponseHandler
import com.xenia.ticket.utils.common.CommonMethod.dismissLoader
import com.xenia.ticket.utils.common.CommonMethod.enableInactivityReset
import com.xenia.ticket.utils.common.CommonMethod.generateNumericTransactionReferenceID
import com.xenia.ticket.utils.common.CommonMethod.isInternetAvailable
import com.xenia.ticket.utils.common.CommonMethod.setLocale
import com.xenia.ticket.utils.common.CommonMethod.showLoader
import com.xenia.ticket.utils.common.CommonMethod.showSnackbar
import com.xenia.ticket.utils.common.CompanyKey
import com.xenia.ticket.utils.common.InactivityHandler
import com.xenia.ticket.utils.common.JwtUtils
import com.xenia.ticket.utils.common.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import retrofit2.HttpException
import java.util.Locale
import kotlin.getValue


class TicketCartActivity : AppCompatActivity(), TicketCartAdapter.OnTicketCartClickListener,
    OnTicketClickListener, CustomInternetAvailabilityDialog.InternetAvailabilityListener,
    CustomInactivityDialog.InactivityCallback,
    InactivityHandlerActivity {

    private lateinit var binding: ActivityTicketCartBinding
    private val ticketRepository: TicketRepository by inject()
    private lateinit var ticketCartAdapter: TicketCartAdapter
    private val sessionManager: SessionManager by inject()
    private val paymentRepository: PaymentRepository by inject()
    private val companyRepository: CompanyRepository by inject()
    private val customInternetAvailabilityDialog: CustomInternetAvailabilityDialog by inject()
    private val customQRDarshanPopupDialogue: CustomQRDarshanPopupDialogue by inject()
    private var formattedTotalAmount: String = ""
    private var idProofType: String? = ""
    private var devoteeName: String? = null
    private var devoteePhone: String? = null
    private var devoteeIdProof: String? = null
    private lateinit var inactivityHandler: InactivityHandler
    private lateinit var inactivityDialog: CustomInactivityDialog
    private lateinit var jwtToken: String
    private var userId: Int = 0
    private var imageData: ByteArray? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTicketCartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setLocale(this, sessionManager.getSelectedLanguage())

        devoteeName = intent.getStringExtra("name")
        devoteePhone = intent.getStringExtra("phno")
        idProofType = intent.getStringExtra("ID")
        devoteeIdProof = intent.getStringExtra("IDtype")

        jwtToken = sessionManager.getToken() ?: throw IllegalStateException("Token missing")
        userId =
            JwtUtils.getUserId(jwtToken) ?: throw IllegalStateException("UserId missing in JWT")
        inactivityDialog = CustomInactivityDialog(this)
        inactivityHandler =
            InactivityHandler(this, supportFragmentManager, inactivityDialog)

        binding.relTicketCart.layoutManager = LinearLayoutManager(this)

        ticketCartAdapter =
            TicketCartAdapter(this, sessionManager.getSelectedLanguage(), "Dharshan", this)

        binding.relTicketCart.adapter = ticketCartAdapter

        initUI()

        binding.btnPay.setOnClickListener {
            val name = binding.editTextName.text.toString()
            val phone = binding.editTextPhoneNumber.text.toString().trim()
            if (phone.isNotEmpty() && phone.length != 10) {
                showMessage("Enter valid phone number")
                return@setOnClickListener
            }

            lifecycleScope.launch {
                ticketRepository.updateCartItemsInfo(
                    newName = name,
                    newPhoneNumber = phone,
                    newIdno = "",
                    newIdProof = "",
                    newImg = imageData ?: ByteArray(0)
                )
            }

            lifecycleScope.launch {

                val amountValue = formattedTotalAmount.toDoubleOrNull() ?: 0.0
                val ctx = this@TicketCartActivity
                val isPaymentGatewayEnabled =
                    companyRepository.getBoolean(CompanyKey.ISPAYMENTGATEWAY)
                binding.btnPay.isEnabled = false

                if (isPaymentGatewayEnabled) {
                    if (amountValue >= 2) {
                        showLoader(ctx, "Generating QR code...")
                        generatePayment()
                    } else {
                        showLoader(ctx, "Generating QR code...")
                        generatePayment()
                    }

                } else {
                    showLoader(ctx, "Posting ticket...")
                    postTicketPaymentHistory("S", "Successful")
                }
            }

        }
        loadTicketItems()
    }

    private fun initUI() {
        binding.txtName.text = getString(R.string.name)
        binding.txtPhoneNumber.text = getString(R.string.phone_number)
        binding.btnPay.text = getString(R.string.pay)
        binding.editTextName.enableInactivityReset(inactivityHandler)
        binding.editTextPhoneNumber.enableInactivityReset(inactivityHandler)

    }

    @SuppressLint("SetTextI18n", "DefaultLocale")
    private fun loadTicketItems() {
        lifecycleScope.launch {
            val allDarshanTickets = ticketRepository.getAllTicketsInCart()
            val (totalAmount) = ticketRepository.getCartStatus()
            if (allDarshanTickets.isEmpty()) {

                val intent = Intent(this@TicketCartActivity, TicketActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()

                finish()
            } else {
                val firstItem = allDarshanTickets.first()

                val bitmap =
                    BitmapFactory.decodeByteArray(firstItem.daImg, 0, firstItem.daImg.size)
                ticketCartAdapter.updateTickets(allDarshanTickets)
                formattedTotalAmount = String.format(Locale.ENGLISH, "%.2f", totalAmount)
                binding.btnPay.text = getString(R.string.pay) + "  Rs. " + formattedTotalAmount
                binding.btnPay.isEnabled = true
                binding.btnPay.setBackgroundColor(
                    ContextCompat.getColor(
                        this@TicketCartActivity,
                        R.color.primaryColor
                    )
                )

            }
        }
    }
    override fun onDeleteClick(ticket: Ticket) {
        lifecycleScope.launch(Dispatchers.IO) {
            ticketRepository.deleteTicketById(ticket.ticketId)
            withContext(Dispatchers.Main) {
                loadTicketItems()
            }
        }
    }

    override fun onEditClick(ticketItem: Ticket) {
        val dialog = CustomTicketPopupDialogue()

        dialog.setData(
            ticketId = ticketItem.ticketId,
            ticketName = ticketItem.ticketName,
            ticketNameMa = ticketItem.ticketNameMa ?: "",
            ticketNameTa = ticketItem.ticketNameTa ?: "",
            ticketNameKa = ticketItem.ticketNameKa ?: "",
            ticketNameTe = ticketItem.ticketNameTe ?: "",
            ticketNameHi = ticketItem.ticketNameHi ?: "",
            ticketNameSi = ticketItem.ticketNameSi ?: "",
            ticketNamePa = ticketItem.ticketNamePa ?: "",
            ticketNameMr = ticketItem.ticketNameMr ?: "",
            ticketCtegoryId = ticketItem.ticketCategoryId,
            ticketCompanyId = ticketItem.ticketCompanyId,
            ticketRate = ticketItem.ticketAmount
        )

        dialog.setListener(this)

        dialog.show(supportFragmentManager, "CustomPopup")
    }

    private fun generatePayment() {
        lifecycleScope.launch {

            val (totalAmount) = ticketRepository.getCartStatus()

            if (!isInternetAvailable(this@TicketCartActivity)) {
                customInternetAvailabilityDialog.show(
                    supportFragmentManager,
                    "warning_dialog"
                )
                return@launch
            }
            val gateway = companyRepository.getString(CompanyKey.PAYMENT_GATEWAY)

            when (gateway?.trim()) {
                "CanaraBank" -> {
                    dismissLoader()
//                    generateCanaraPaymentQrCode(formattedTotalAmount)
                }
                "FederalBank" -> {
                    generateFederalPaymentQrCode(totalAmount)
                }
                else -> {
                    dismissLoader()
                    generateSibQrCode(totalAmount)
                }
            }
            if (gateway.isNullOrEmpty()) {
            dismissLoader()
            return@launch
        }


        }
    }
    private fun generateFederalPaymentQrCode(donationAmount: Double) {
        val request = FedQrRequest(
            Amount = donationAmount.toInt(),
            name ="",
            phone = "",
        )

        CoroutineScope(Dispatchers.Main).launch {
            try {

                val response = withContext(Dispatchers.IO) {
                    paymentRepository.generateFedQr(
                        token = sessionManager.getToken().toString(),
                        request = request
                    )
                }

                val orderId = response.OrderId
                val upiUrl = response.UpiIntentUrl
                val name = binding.editTextName.text.toString().trim()
                val phone = binding.editTextPhoneNumber.text.toString().trim()
                if (!orderId.isNullOrEmpty() && !upiUrl.isNullOrEmpty()) {
                    customQRDarshanPopupDialogue.setData(
                        donationAmount.toInt().toString(),
                        upiUrl,
                        orderId,
                        name,
                        phone

                    )
                    dismissLoader()
                    customQRDarshanPopupDialogue.show(
                        supportFragmentManager,
                        "CustomPopup"
                    )
                }
            } catch (e: HttpException) {
                showSnackbar(
                    binding.root,
                    "unable to generate QR code! Please try again..."
                )
            }
        }
    }

    private fun generateSibQrCode(donationAmount: Double) {

        val transactionId = generateNumericTransactionReferenceID()

        val paymentRequest = SibQrRequest(
            transactionReferenceID = transactionId,
            amount = donationAmount.toString(),
            name = "",
            phoneNumber = ""
        )

        lifecycleScope.launch {
            try {

                val response = paymentRepository.generateSibQr(
                    token = sessionManager.getToken().toString(),
                    payFor = "Common",
                    request = paymentRequest
                )
                val orderId = response.transactionReferenceId
                val upiUrl = response.intentUrl
                val name = binding.editTextName.text.toString().trim()
                val phone = binding.editTextPhoneNumber.text.toString().trim()
                if (!orderId.isNullOrEmpty() && !upiUrl.isNullOrEmpty()) {
                    customQRDarshanPopupDialogue.setData(
                        donationAmount.toInt().toString(),
                        upiUrl,
                        orderId,
                        name,
                        phone

                    )
                    dismissLoader()
                    customQRDarshanPopupDialogue.show(
                        supportFragmentManager,
                        "CustomPopup"
                    )
                } else {
                    dismissLoader()
                    showSnackbar(binding.root, "Unable to generate QR code!")
                }

            } catch (e: Exception) {
                dismissLoader()
                showSnackbar(binding.root, "Something went wrong!")
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
            if (cartTickets.isEmpty()) {
                withContext(Dispatchers.Main) {
                    binding.btnPay.isEnabled = true
                }
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
                firstTicket.daImg ?: ByteArray(0),
                Base64.NO_WRAP
            )


            val token = sessionManager.getToken().toString()
            val companyId = JwtUtils.getCompanyId(token)

            if (token.isNullOrBlank()) {
                withContext(Dispatchers.Main) {
                    binding.btnPay.isEnabled = true
                    showSnackbar(binding.root, "Authorization token missing")
                }
                return
            }

            val name = binding.editTextName.text.toString().trim()
            val phone = binding.editTextPhoneNumber.text.toString().trim()

            val request = TicketPaymentRequest(
                CompanyId = companyId!!,
                UserId = userId,
                Name = name,
                tTranscationId = generateNumericTransactionReferenceID(),
                tCustRefNo = "",
                tNpciTransId = "",
                tIdProofNo = "",
                tImage = imageBase64String,
                PhoneNumber = phone,
                tPaymentStatus = status,
                tPaymentMode = "CASH",
                tPaymentDes = statusDesc,
                Items = itemsList
            )


            ApiResponseHandler.handleApiCall(
                activity = this@TicketCartActivity,
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
                            binding.btnPay.isEnabled = true
                            dismissLoader()
                            handleTicketTransactionStatus(
                                status = "S",
                                orderId = response.receipt,
                                ticket = response.ticket,
                                totalAmount = totalAmount,
                                companyRepository.getString(CompanyKey.PREFIX) ?: ""
                            )
                        }

                    } else {
                        binding.btnPay.isEnabled = true
                        dismissLoader()
                        showSnackbar(binding.root, "Failed to post order")
                    }
                }
            )

        } catch (e: HttpException) {
            withContext(Dispatchers.Main) {
                if (e.code() == 401) {
                    dismissLoader()
                    binding.btnPay.isEnabled = true
                    AlertDialog.Builder(this@TicketCartActivity)
                        .setTitle("Logout !!")
                        .setMessage("You have been logged out because your account was used on another device.")
                        .setCancelable(false)
                        .setPositiveButton("Logout") { _, _ ->
                            ApiResponseHandler.logoutUser(this@TicketCartActivity)
                        }
                        .show()
                } else {
                    handleRetry(e, status, statusDesc, retryCount)
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                dismissLoader()
                binding.btnPay.isEnabled = true
                showSnackbar(binding.root, "Something went wrong")
            }
        }
    }

    private fun handleRetry(
        e: Exception,
        status: String,
        statusDesc: String,
        retryCount: Int,

        ) {
        if (e is HttpException && e.code() == 401) {
            binding.btnPay.isEnabled = true
            showSnackbar(binding.root, "Unauthorized: Please login again")
            return
        }

        if (retryCount < 3) {
            lifecycleScope.launch {
                postTicketPaymentHistory(status, statusDesc, retryCount + 1)
            }
        } else {
            binding.btnPay.isEnabled = true
            dismissLoader()
            showSnackbar(binding.root, "Failed: ${e.message}")
        }
    }

    private fun handleTicketTransactionStatus(
        status: String,
        orderId: String,
        ticket: String?,
        totalAmount: Double,
        receiptPrefix: String?
    ) {

        val intent = Intent(this, PaymentActivity::class.java).apply {

            putExtra("status", status)
            putExtra("amount", totalAmount.toString())
            putExtra("orderID", orderId)
            putExtra("transID", "")
            putExtra("ticket", ticket)
            putExtra("prefix", receiptPrefix)
            putExtra("name", binding.editTextName.text.toString())
            putExtra("phno", binding.editTextPhoneNumber.text.toString())

        }

        startActivity(intent)
        finish()
    }

    fun showMessage(msg: String) {
        AlertDialog.Builder(this)
            .setMessage(msg)
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onTicketClick(darshanItem: TicketDto) {
        TODO("Not yet implemented")
    }

    override fun onTicketClear(darshanItem: TicketDto) {
        TODO("Not yet implemented")
    }

    override fun onTicketAdded() {
        loadTicketItems()
    }

    override fun onResume() {
        super.onResume()
        inactivityHandler.resumeInactivityCheck()
    }

    override fun onPause() {
        super.onPause()
        inactivityHandler.pauseInactivityCheck()
    }

    override fun onDestroy() {
        super.onDestroy()
        inactivityHandler.cleanup()
    }

    override fun onRetryClicked() {
        inactivityHandler.resumeInactivityCheck()
        //generatePaymentToken(donationAmount!!)
    }

    override fun onDialogInactive() {
        inactivityHandler.resumeInactivityCheck()
        inactivityHandler.showDialogSafely()
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        inactivityHandler.resetTimer()
        return super.dispatchTouchEvent(ev)
    }

    override fun resetInactivityTimer() {
        inactivityHandler.resetTimer()
    }

}