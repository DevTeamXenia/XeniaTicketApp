package com.xenia.ticket.ui.screens.billing

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.MotionEvent
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.xenia.ticket.R
import com.xenia.ticket.data.listeners.OnTicketClickListener
import com.xenia.ticket.data.network.model.TicketDto
import com.xenia.ticket.data.network.model.TicketPaymentRequest
import com.xenia.ticket.data.repository.CompanyRepository
import com.xenia.ticket.data.repository.PaymentRepository
import com.xenia.ticket.data.repository.TicketRepository
import com.xenia.ticket.data.room.entity.Ticket
import com.xenia.ticket.databinding.ActivityBillingCartBinding
import com.xenia.ticket.ui.adapter.TicketCartAdapter
import com.xenia.ticket.ui.dialog.CustomInternetAvailabilityDialog
import com.xenia.ticket.ui.dialog.CustomTicketPopupDialogue
import com.xenia.ticket.ui.screens.kiosk.PaymentActivity
import com.xenia.ticket.ui.screens.kiosk.TicketActivity
import com.xenia.ticket.utils.common.ApiResponseHandler
import com.xenia.ticket.utils.common.CommonMethod.dismissLoader
import com.xenia.ticket.utils.common.CommonMethod.generateNumericTransactionReferenceID
import com.xenia.ticket.utils.common.CommonMethod.isInternetAvailable
import com.xenia.ticket.utils.common.CommonMethod.setLocale
import com.xenia.ticket.utils.common.CommonMethod.showLoader
import com.xenia.ticket.utils.common.CommonMethod.showSnackbar
import com.xenia.ticket.utils.common.CompanyKey
import com.xenia.ticket.utils.common.Constants.CARD
import com.xenia.ticket.utils.common.Constants.CASH
import com.xenia.ticket.utils.common.Constants.UPI
import com.xenia.ticket.utils.common.JwtUtils
import com.xenia.ticket.utils.common.PlutusConstants
import com.xenia.ticket.utils.common.PlutusServiceManager
import com.xenia.ticket.utils.common.SessionManager

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.koin.android.ext.android.inject
import retrofit2.HttpException
import java.util.Locale
import kotlin.getValue

class BillingCartActivity : AppCompatActivity(), TicketCartAdapter.OnTicketCartClickListener,
    OnTicketClickListener,
    CustomInternetAvailabilityDialog.InternetAvailabilityListener {

    private lateinit var binding: ActivityBillingCartBinding
    private val ticketRepository: TicketRepository by inject()
    private lateinit var ticketCartAdapter: TicketCartAdapter
    private val sessionManager: SessionManager by inject()
    private val paymentRepository: PaymentRepository by inject()
    private val customInternetAvailabilityDialog: CustomInternetAvailabilityDialog by inject()
    private val companyRepository: CompanyRepository by inject()
    private var selectedPaymentMode: String = ""
    private var formattedTotalAmount: String = ""
    private var selectedLanguage: String? = ""
    private val customTicketPopupDialogue: CustomTicketPopupDialogue by inject()
    private lateinit var ticketItemsItems: Ticket

    private lateinit var jwtToken: String
    private var userId: Int = 0
    private var imageData: ByteArray? = null
    private lateinit var plutusManager: PlutusServiceManager
    private var transactionId: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBillingCartBinding.inflate(layoutInflater)
        setContentView(binding.root)
        selectedLanguage = sessionManager.getBillingSelectedLanguage()
        setLocale(this, selectedLanguage)

        if (intent.getBooleanExtra("forceLandscape", false)) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }

        plutusManager = PlutusServiceManager(this) { response ->
            val jsonResponse = JSONObject(response)
            Log.d("PlutusResponse", response)
            val responseObj = jsonResponse.optJSONObject("Response")
            val statusMsg = responseObj?.optString("ResponseMsg", "") ?: ""

            if (statusMsg.equals("APPROVED", ignoreCase = true)) {
                lifecycleScope.launch {
                    dismissLoader()
                    postTicketPaymentHistory("S", "Successful")
                }
            }else{
                dismissLoader()
                binding.btnPay.isEnabled = true
                Toast.makeText(this, "Something went wrong please try again...", Toast.LENGTH_SHORT).show()
            }
        }
        plutusManager.bindService()
        initView()
        binding.relTicketCart.layoutManager = LinearLayoutManager(this)
        ticketCartAdapter =
            TicketCartAdapter(this, sessionManager.getBillingSelectedLanguage(), "Booking", this)

        binding.relTicketCart.adapter = ticketCartAdapter

        loadDarshanItems()
    }
    private fun initView() {
        binding.radioGroup.check(R.id.radiaCash)
        binding.radiaCash.setBackgroundResource(R.drawable.selected_border)
        binding.radiaUPI.setBackgroundResource(R.drawable.editext_boarder)
        binding.radiaCard.setBackgroundResource(R.drawable.editext_boarder)
        binding.btnPay.isEnabled = true
        selectedPaymentMode = CASH
        binding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radiaUPI -> {
                    binding.radiaUPI.setBackgroundResource(R.drawable.selected_border)
                    binding.radiaCash.setBackgroundResource(R.drawable.editext_boarder)
                    binding.radiaCard.setBackgroundResource(R.drawable.editext_boarder)
                    selectedPaymentMode = UPI
                }

                R.id.radiaCash -> {
                    binding.radiaCash.setBackgroundResource(R.drawable.selected_border)
                    binding.radiaUPI.setBackgroundResource(R.drawable.editext_boarder)
                    binding.radiaCard.setBackgroundResource(R.drawable.editext_boarder)
                    selectedPaymentMode = CASH
                }

                R.id.radiaCard -> {
                    binding.radiaCard.setBackgroundResource(R.drawable.selected_border)
                    binding.radiaUPI.setBackgroundResource(R.drawable.editext_boarder)
                    binding.radiaCash.setBackgroundResource(R.drawable.editext_boarder)
                    selectedPaymentMode = CARD
                }
            }
        }
        binding.txtName.text = getString(R.string.name)
        binding.txtPhoneNumber.text = getString(R.string.phone_number)
        binding.btnPay.text = getString(R.string.pay)
        selectedPaymentMode = CASH
        jwtToken = sessionManager.getToken() ?: throw IllegalStateException("Token missing")
        userId =
            JwtUtils.getUserId(jwtToken) ?: throw IllegalStateException("UserId missing in JWT")

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
            if (!isInternetAvailable(this)) {
                dismissLoader()
                showSnackbar(
                    binding.root,
                    applicationContext.getString(R.string.no_internet_connection)
                )
                if (!customInternetAvailabilityDialog.isAdded && !isFinishing && !isDestroyed) {
                    customInternetAvailabilityDialog.show(
                        supportFragmentManager,
                        "internet_availability_dialog"
                    )
                }
                return@setOnClickListener
            }
            binding.btnPay.isEnabled = false

            lifecycleScope.launch {
                val ctx = this@BillingCartActivity
                if (selectedPaymentMode == CASH) {
                    showLoader(ctx,  "Generating ticket...")
                    postTicketPaymentHistory("S", "Successful")
                    return@launch
                } else if (companyRepository.getBoolean(CompanyKey.ISPAYMENTGATEWAY)) {
                    showLoader(ctx,  "Initiate Payment...")
                    generatePayment()
                }
            }
        }
    }
    private fun generatePayment() {
        lifecycleScope.launch {

            val (totalAmount) = ticketRepository.getCartStatus()

            if (!isInternetAvailable(this@BillingCartActivity)) {
                customInternetAvailabilityDialog.show(
                    supportFragmentManager,
                    "warning_dialog"
                )
                return@launch
            }
            val gateway = companyRepository.getString(CompanyKey.PAYMENT_GATEWAY)

            when (gateway) {
                "CanaraBank" -> {
                    dismissLoader()
//                    generateCanaraPaymentQrCode(formattedTotalAmount)
                }

                "FederalBank" -> {
                    //generateFederalPaymentQrCode(totalAmount)
                }

                "PineLabs" -> {

                    generatePineLabPaymentQrCode(totalAmount)
                }

                else -> {
                    dismissLoader()
//                    generatePaymentQrCode(formattedTotalAmount)
                }
            }

        }
    }
    private fun generatePineLabPaymentQrCode(totalAmount: Double) {

        if (!::plutusManager.isInitialized) {
            showSnackbar(binding.root, "Payment service not ready")
            dismissLoader()
            return
        }
        val transactionType = when (selectedPaymentMode) {
            CARD -> 4001
            else -> 5120
        }
        transactionId=generateNumericTransactionReferenceID()
        val request = JSONObject().apply {
            val formattedAmount = totalAmount * 100
            put("Header", JSONObject().apply {
                put("ApplicationId", sessionManager.getPineLabsAppId())
                put("UserId", "cashier1")
                put("MethodId", PlutusConstants.METHOD_DO_TRANSACTION)
                put("VersionNo", "1.0")
            })

            put("Detail", JSONObject().apply {
                put("TransactionType", transactionType)
                put("BillingRefNo", transactionId)
                put("PaymentAmount", formattedAmount.toString())
            })
        }
        Log.d("PlutusRequest", request.toString())
        plutusManager.sendRequest(request.toString())
    }


    @SuppressLint("SetTextI18n", "DefaultLocale")
    private fun loadDarshanItems() {
        lifecycleScope.launch {
            val allDarshanTickets = ticketRepository.getAllTicketsInCart()
            val (totalAmount) = ticketRepository.getCartStatus()
            if (allDarshanTickets.isEmpty()) {
                startActivity(Intent(this@BillingCartActivity, TicketActivity::class.java))
                finish()
            } else {
                val firstItem = allDarshanTickets.first()
                val bitmap = BitmapFactory.decodeByteArray(firstItem.daImg, 0, firstItem.daImg.size)
                ticketCartAdapter.updateTickets(allDarshanTickets)
                formattedTotalAmount = String.format(Locale.ENGLISH, "%.2f", totalAmount)
                binding.btnPay.text = getString(R.string.pay) + "  Rs. " + formattedTotalAmount
                binding.btnPay.isEnabled = true
                binding.btnPay.setBackgroundColor(
                    ContextCompat.getColor(
                        this@BillingCartActivity,
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
                loadDarshanItems()
            }
        }
    }

    override fun onEditClick(ticket: Ticket) {

        ticketItemsItems = ticket

        customTicketPopupDialogue.setData(
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
            ticketCtegoryId = ticket.ticketCategoryId,
            ticketCompanyId = ticket.ticketCompanyId,
            ticketRate = ticket.ticketAmount
        )
        customTicketPopupDialogue.setListener(this)
        if (!customTicketPopupDialogue.isAdded) {
            customTicketPopupDialogue.show(
                supportFragmentManager,
                "CustomPopup"
            )
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
                tTranscationId = transactionId.toString(),
                tCustRefNo = "",
                tNpciTransId = "",
                tIdProofNo = "",
                tImage = imageBase64String,
                PhoneNumber = phone,
                tPaymentStatus = status,
                tPaymentMode = selectedPaymentMode,
                tPaymentDes = statusDesc,
                Items = itemsList
            )
            ApiResponseHandler.handleApiCall(
                activity = this@BillingCartActivity,
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
                                companyRepository.getString(CompanyKey.PREFIX) ?: ""
                            )
                        }

                    } else {
                        binding.btnPay.isEnabled = true
                        showSnackbar(binding.root, "Failed to post order")
                    }
                }
            )

        } catch (e: HttpException) {

            withContext(Dispatchers.Main) {
                if (e.code() == 401) {
                    AlertDialog.Builder(this@BillingCartActivity)
                        .setTitle("Logout !!")
                        .setMessage("You have been logged out because your account was used on another device.")
                        .setCancelable(false)
                        .setPositiveButton("Logout") { _, _ ->
                            ApiResponseHandler.logoutUser(this@BillingCartActivity)
                        }
                        .show()
                } else {
                    handleRetry(e, status, statusDesc, retryCount)
                }
            }

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                binding.btnPay.isEnabled = true
                showSnackbar(binding.root, "Something went wrong")
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
            putExtra("from", "billing")
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

    override fun onTicketClick(ticketItem: TicketDto) {
        TODO("Not yet implemented")
    }

    override fun onTicketClear(ticketItem: TicketDto) {
        TODO("Not yet implemented")
    }

    override fun onTicketAdded() {
        loadDarshanItems()
    }

    override fun onResume() {
        super.onResume()

    }

    override fun onPause() {
        super.onPause()
        currentFocus?.clearFocus()
    }


    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onRetryClicked() {
        loadDarshanItems()
    }

    override fun onDialogInactive() {
        loadDarshanItems()
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        return super.dispatchTouchEvent(ev)
    }

    fun showMessage(msg: String) {
        AlertDialog.Builder(this)
            .setMessage(msg)
            .setPositiveButton("OK", null)
            .show()
    }

}