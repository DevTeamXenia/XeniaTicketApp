package com.xenia.ticket.ui.screens.kiosk

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Base64
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.xenia.ticket.R
import com.xenia.ticket.data.listeners.InactivityHandlerActivity
import com.xenia.ticket.data.listeners.OnTicketClickListener
import com.xenia.ticket.data.network.model.FedQrRequest
import com.xenia.ticket.data.network.model.SibQrRequest
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
import androidx.core.graphics.drawable.toDrawable
import com.xenia.ticket.data.network.model.ActiveItem


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
            TicketCartAdapter(this, sessionManager.getSelectedLanguage(), "Ticket", this)

        binding.relTicketCart.adapter = ticketCartAdapter

        initUI()

        binding.btnPay.setOnClickListener {
            val name = binding.editTextName.text.toString()
            val phone = binding.editTextPhoneNumber.text.toString().trim()

            if (name.isEmpty()) {
                binding.editTextName.error = "Name is required"
                binding.editTextName.requestFocus()
                binding.editTextName.post {
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showSoftInput(binding.editTextName, InputMethodManager.SHOW_IMPLICIT)
                }
                return@setOnClickListener
            }

            if (phone.isEmpty()) {
                binding.editTextPhoneNumber.error = "Phone number is required"
                binding.editTextPhoneNumber.requestFocus()
                binding.editTextPhoneNumber.post {
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showSoftInput(binding.editTextPhoneNumber, InputMethodManager.SHOW_IMPLICIT)
                }
                return@setOnClickListener
            }
            if (phone.length < 10) {
                binding.editTextPhoneNumber.error =
                    "Enter valid phone number with at least 10 digits"
                binding.editTextPhoneNumber.requestFocus()
                binding.editTextPhoneNumber.post {
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showSoftInput(binding.editTextPhoneNumber, InputMethodManager.SHOW_IMPLICIT)
                }
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
        binding.editTextName.requestFocus()
        binding.editTextName.post {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.editTextName, InputMethodManager.SHOW_IMPLICIT)
        }
        binding.txtPhoneNumber.text = getString(R.string.phone_number)
        binding.btnPay.text = getString(R.string.pay)
        binding.editTextName.enableInactivityReset(inactivityHandler)
        binding.editTextPhoneNumber.enableInactivityReset(inactivityHandler)
        binding.ivBack.setOnClickListener {
            val intent = Intent(applicationContext, TicketActivity::class.java)
            startActivity(intent)

        }
        binding.linHome.setOnClickListener {
            val dialog = Dialog(this)
            dialog.setContentView(R.layout.dialog_quit_cart)
            dialog.setCancelable(false)
            dialog.window?.apply {
                setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            }
            val btnYes = dialog.findViewById<MaterialButton>(R.id.buttonyes)
            val btnNo = dialog.findViewById<MaterialButton>(R.id.buttonCancel)

            btnYes.setOnClickListener {
                lifecycleScope.launch {
                    ticketRepository.clearAllData()
                }
                dialog.dismiss()
                finish()
            }

            btnNo.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        }
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
                ticketCartAdapter.updateTickets(allDarshanTickets)
                formattedTotalAmount = String.format(Locale.ENGLISH, "%.2f", totalAmount)

                if(sessionManager.getSelectedLanguage() == "te")
                    binding.btnPay.text =
                        "${getString(R.string.rs)} $formattedTotalAmount ${getString(R.string.proceed)}"
                else
                    binding.btnPay.text =
                        getString(R.string.pay) + "  Rs.$formattedTotalAmount"

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

    override fun onEditClick(ticket: Ticket) {
        val dialog = CustomTicketPopupDialogue()
        dialog.setData(
            ticketId = ticket.ticketId,
            ticketName = ticket.ticketName,
            ticketNameMa = ticket.ticketNameMa ?: "",
            ticketNameTa = ticket.ticketNameTa ?: "",
            ticketNameKa = ticket.ticketNameKa ?: "",
            ticketNameTe = ticket.ticketNameTe ?: "",
            ticketNameHi = ticket.ticketNameHi ?: "",
            ticketNameSi = ticket.ticketNameSi ?: "",
            ticketNamePa = ticket.ticketNamePa ?: "",
            ticketNameMr = ticket.ticketNameMr ?: "",
            ticketCategoryId = ticket.ticketCategoryId,
            ticketCompanyId = ticket.ticketCompanyId,
            ticketRate = ticket.ticketAmount,
            ticketCombo = false
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
                "FederalBank" -> {
                    when {
                        totalAmount == 0.0 -> {
                            dismissLoader()
                            postTicketPaymentHistory(
                                status = "S",
                                statusDesc = "ZERO AMOUNT PAYMENT"
                            )
                            return@launch
                        }

                        totalAmount == 1.0 -> {
                            dismissLoader()
                            showSnackbar(
                                findViewById(android.R.id.content),
                                "Please select an amount greater than 1"
                            )
                            return@launch
                        }

                        totalAmount >= 2.0 -> {
                            generateFederalPaymentQrCode(totalAmount)
                        }
                    }
                }
                else -> {
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
            name = "",
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
                    binding.btnPay.isEnabled = true
                    customQRDarshanPopupDialogue.show(
                        supportFragmentManager,
                        "CustomPopup"
                    )
                }
            } catch (e: HttpException) {
                if (e.code() == 401) {
                    AlertDialog.Builder(this@TicketCartActivity)
                        .setTitle("Logout !!")
                        .setMessage(
                            "You have been logged out because your account was used on another device."
                        )
                        .setCancelable(false)
                        .setPositiveButton("Logout") { _, _ ->
                            ApiResponseHandler.logoutUser(this@TicketCartActivity)
                        }
                        .show()
                } else {
                    showSnackbar(binding.root, "Unable to load settings!")
                }
            } catch (_: HttpException) {
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

                    // ✅ Prevent duplicate dialog
                    val existingDialog =
                        supportFragmentManager.findFragmentByTag("CustomPopup")

                    if (existingDialog != null && existingDialog.isAdded) {
                        return@launch
                    }

                    val dialog = CustomQRDarshanPopupDialogue().apply {
                        setData(
                            donationAmount.toInt().toString(),
                            upiUrl,
                            orderId,
                            name,
                            phone
                        )
                    }

                    dismissLoader()

                    // ✅ SAFETY CHECK (VERY IMPORTANT)
                    if (!isFinishing && !supportFragmentManager.isStateSaved) {
                        dialog.show(supportFragmentManager, "CustomPopup")
                    }

                } else {
                    dismissLoader()
                    showSnackbar(binding.root, "Unable to generate QR code!")
                }

            } catch (e: HttpException) {

                dismissLoader()

                if (e.code() == 401) {
                    AlertDialog.Builder(this@TicketCartActivity)
                        .setTitle("Logout !!")
                        .setMessage("You have been logged out because your account was used on another device.")
                        .setCancelable(false)
                        .setPositiveButton("Logout") { _, _ ->
                            ApiResponseHandler.logoutUser(this@TicketCartActivity)
                        }
                        .show()
                } else {
                    showSnackbar(binding.root, "Unable to load settings!")
                }

            } catch (_: Exception) {

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
                firstTicket.daImg,
                Base64.NO_WRAP
            )

            val token = sessionManager.getToken().toString()
            val companyId = JwtUtils.getCompanyId(token)

            if (token.isBlank()) {
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

            // ✅ NEW suspend-based call (IMPORTANT)
            val response = ApiResponseHandler.handleApiCall(
                activity = this@TicketCartActivity
            ) {
                paymentRepository.postTicket(
                    bearerToken = "Bearer $token",
                    request = request
                )
            }

            val (totalAmount) = ticketRepository.getCartStatus()

            withContext(Dispatchers.Main) {

                dismissLoader()
                binding.btnPay.isEnabled = true

                if (response != null &&
                    response.status.equals("Success", ignoreCase = true) &&
                    !response.receipt.isNullOrBlank()
                ) {
                    handleTicketTransactionStatus(
                        orderId = response.receipt,
                        ticket = response.ticket,
                        totalAmount = totalAmount,
                        companyRepository.getString(CompanyKey.PREFIX) ?: ""
                    )
                } else {
                    showSnackbar(binding.root, "Failed to post order")
                }
            }

        } catch (_: Exception) {
            if (retryCount < 3) {
                postTicketPaymentHistory(status, statusDesc, retryCount + 1)
            } else {
                withContext(Dispatchers.Main) {
                    dismissLoader()
                    binding.btnPay.isEnabled = true
                    showSnackbar(binding.root, "Something went wrong")
                }
            }
        }
    }


    private fun handleTicketTransactionStatus(
        orderId: String,
        ticket: String?,
        totalAmount: Double,
        receiptPrefix: String?
    ) {

        val intent = Intent(this, PaymentActivity::class.java).apply {

            putExtra("status", "S")
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


    override fun onTicketClick(item: ActiveItem) {
        TODO("Not yet implemented")
    }

    override fun onTicketClear(item: ActiveItem) {
        TODO("Not yet implemented")
    }

    override fun onTicketAdded(ticketId: Int) {
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