package com.example.ticket.ui.sreens.billing

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.MotionEvent
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ticket.R
import com.example.ticket.data.listeners.OnTicketClickListener
import com.example.ticket.data.network.model.TicketDto
import com.example.ticket.data.network.model.TicketPaymentRequest
import com.example.ticket.data.repository.CompanyRepository
import com.example.ticket.data.repository.PaymentRepository
import com.example.ticket.data.repository.TicketRepository
import com.example.ticket.data.room.entity.Ticket
import com.example.ticket.databinding.ActivityBillingCartBinding
import com.example.ticket.ui.adapter.TicketCartAdapter
import com.example.ticket.ui.dialog.CustomInternetAvailabilityDialog
import com.example.ticket.ui.dialog.CustomTicketPopupDialogue
import com.example.ticket.ui.sreens.screen.PaymentActivity
import com.example.ticket.ui.sreens.screen.TicketActivity
import com.example.ticket.utils.common.ApiResponseHandler
import com.example.ticket.utils.common.CommonMethod.dismissLoader
import com.example.ticket.utils.common.CommonMethod.generateNumericTransactionReferenceID
import com.example.ticket.utils.common.CommonMethod.isInternetAvailable
import com.example.ticket.utils.common.CommonMethod.setLocale
import com.example.ticket.utils.common.CommonMethod.showSnackbar
import com.example.ticket.utils.common.CompanyKey
import com.example.ticket.utils.common.JwtUtils
import com.example.ticket.utils.common.SessionManager

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import retrofit2.HttpException
import java.util.Locale
import kotlin.getValue


class BillingCartActivity : AppCompatActivity(), TicketCartAdapter.OnTicketCartClickListener,
    OnTicketClickListener,
    CustomInternetAvailabilityDialog.InternetAvailabilityListener{

    private lateinit var binding: ActivityBillingCartBinding
    private val ticketRepository: TicketRepository by inject()
    private lateinit var ticketCartAdapter: TicketCartAdapter
    private val sessionManager: SessionManager by inject()
    private val paymentRepository: PaymentRepository by inject()
    private val customInternetAvailabilityDialog: CustomInternetAvailabilityDialog by inject()
    private val companyRepository: CompanyRepository by inject()

    private var formattedTotalAmount: String = ""
    private var selectedLanguage: String? = ""
    private val customTicketPopupDialogue: CustomTicketPopupDialogue by inject()
    private lateinit var ticketItemsItems: Ticket

    private lateinit var jwtToken: String
    private var userId: Int = 0
    private var imageData: ByteArray? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBillingCartBinding.inflate(layoutInflater)
        setContentView(binding.root)
        selectedLanguage = sessionManager.getBillingSelectedLanguage()
        setLocale(this, selectedLanguage)

        if (intent.getBooleanExtra("forceLandscape", false)) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }

        initView()

        binding.relTicketCart.layoutManager = LinearLayoutManager(this)

        ticketCartAdapter =
            TicketCartAdapter(this, sessionManager.getBillingSelectedLanguage(), "Booking", this)

        binding.relTicketCart.adapter = ticketCartAdapter

        loadDarshanItems()

    }
    private fun initView() {
        binding.txtName.text = getString(R.string.name)
        binding.txtPhoneNumber.text = getString(R.string.phone_number)
        binding.btnPay.text = getString(R.string.pay)
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
                postTicketPaymentHistory(
                    status = "S",
                    statusDesc = "Payment Success"
                )
            }
        }
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
    override fun onEditClick(ticketItem: Ticket) {

        ticketItemsItems = ticketItem

        customTicketPopupDialogue.setData(
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
                firstTicket.daImg ?: ByteArray(0),
                Base64.NO_WRAP
            )

            // ✅ suspend call – now valid
            val companyId = companyRepository.getCompany()?.companyId ?: 0
            val token = sessionManager.getToken()

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
                CompanyId = companyId,
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

            Log.d("PAYMENT_DEBUG", "Posting ${itemsList.size} items")

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

    override fun onTicketClick(darshanItem: TicketDto) {
        TODO("Not yet implemented")
    }

    override fun onTicketClear(darshanItem: TicketDto) {
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
    override fun onBackPressed() {
        val intent = Intent(this, BillingTicketActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}