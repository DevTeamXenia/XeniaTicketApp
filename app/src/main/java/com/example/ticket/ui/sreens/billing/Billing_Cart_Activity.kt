package com.example.ticket.ui.sreens.billing

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.MotionEvent
import android.view.View
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
import com.example.ticket.utils.common.CommonMethod.dismissLoader
import com.example.ticket.utils.common.CommonMethod.generateNumericTransactionReferenceID
import com.example.ticket.utils.common.CommonMethod.isInternetAvailable
import com.example.ticket.utils.common.CommonMethod.setLocale
import com.example.ticket.utils.common.CommonMethod.showSnackbar
import com.example.ticket.utils.common.JwtUtils
import com.example.ticket.utils.common.SessionManager

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import retrofit2.HttpException
import kotlin.getValue


class Billing_Cart_Activity : AppCompatActivity(), TicketCartAdapter.OnTicketCartClickListener,
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
    private var idProofType : String? = ""
    private val customTicketPopupDialogue: CustomTicketPopupDialogue by inject()
    private lateinit var ticketItemsItems: Ticket

    private lateinit var jwtToken: String
    private var userId: Int = 0

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
            TicketCartAdapter(this, sessionManager.getSelectedLanguage(), "Booking", this)

        binding.relTicketCart.adapter = ticketCartAdapter

        loadDarshanItems()

    }
    private fun initView() {
        jwtToken = sessionManager.getToken() ?: throw IllegalStateException("Token missing")
        userId =
            JwtUtils.getUserId(jwtToken) ?: throw IllegalStateException("UserId missing in JWT")
        binding.btnPay.setOnClickListener {
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
            postTicketPaymentHistory("S", "Successful")
        }
    }
    @SuppressLint("SetTextI18n", "DefaultLocale")
    private fun loadDarshanItems() {
        lifecycleScope.launch {
            val allDarshanTickets = ticketRepository.getAllTicketsInCart()
            val (totalAmount) = ticketRepository.getCartStatus()
            if (allDarshanTickets.isEmpty()) {
                startActivity(Intent(this@Billing_Cart_Activity, TicketActivity::class.java))
                finish()
            } else {
                val firstItem = allDarshanTickets.first()
                binding.txtValName.text = firstItem.daName
                binding.txtValPhNo.text = firstItem.daPhoneNumber
                binding.txtValIdNo.text = firstItem.daProofId
                binding.txtIdNo.text = "$idProofType:"
                val bitmap = BitmapFactory.decodeByteArray(firstItem.daImg, 0, firstItem.daImg.size)
                binding.personImage.setImageBitmap(bitmap)
                binding.personImage.visibility = View.VISIBLE
                ticketCartAdapter.updateTickets(allDarshanTickets)
                formattedTotalAmount = String.format("%.2f", totalAmount)
                binding.btnPay.text = getString(R.string.pay) + "  Rs. " + formattedTotalAmount
                binding.btnPay.isEnabled = true
                binding.btnPay.setBackgroundColor(
                    ContextCompat.getColor(
                        this@Billing_Cart_Activity,
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





    private fun postTicketPaymentHistory(
        status: String,
        statusDesc: String,
        retryCount: Int = 0
    ) {
        lifecycleScope.launch {

            val cartTickets = ticketRepository.getAllTicketsInCart()
            if (cartTickets.isEmpty()) return@launch
            val itemsList = cartTickets.map { item ->
                TicketPaymentRequest.Item(
                    taCategoryId = item.ticketCategoryId,
                    TicketId = item.ticketId,
                    Quantity = item.daQty,
                    Rate = item.daRate
                )
            }
            val firstTicket = cartTickets.firstOrNull()
            val byteArray = firstTicket?.daImg ?: ByteArray(0)
            val imageBase64String =
                Base64.encodeToString(byteArray, Base64.NO_WRAP)

            val companyId = companyRepository.getCompany()?.companyId ?: 0
            val token = sessionManager.getToken()

            if (token.isNullOrBlank()) {
                binding.btnPay.isEnabled = true
                showSnackbar(binding.root, "Authorization token missing")
                return@launch
            }
            val request = TicketPaymentRequest(
                CompanyId = companyId,
                UserId = userId,
                Name = firstTicket?.daName.orEmpty(),
                tTranscationId = generateNumericTransactionReferenceID(),
                tCustRefNo = firstTicket?.daCustRefNo.orEmpty(),
                tNpciTransId = firstTicket?.daNpciTransId.orEmpty(),
                tIdProofNo = firstTicket?.daProofId.orEmpty(),
                tImage = imageBase64String,
                PhoneNumber = firstTicket?.daPhoneNumber.orEmpty(),
                tPaymentStatus = status,
                tPaymentMode = "CASH",
                tPaymentDes = statusDesc,
                Items = itemsList
            )

            try {
                val response = withContext(Dispatchers.IO) {
                    paymentRepository.postTicket(
                        bearerToken = "Bearer $token",
                        request = request
                    )
                }

                if (
                    response.status.equals("Success", ignoreCase = true) &&
                    !response.receipt.isNullOrBlank()
                ) {

                    val (totalAmount) = ticketRepository.getCartStatus()

                    handleTicketTransactionStatus(
                        status = "S",
                        orderId = response.receipt,
                        ticket = response.ticket,
                        totalAmount = totalAmount.toDouble()
                    )

                } else {
                    binding.btnPay.isEnabled = true
                    showSnackbar(binding.root, "Failed to post order")
                }

            } catch (e: HttpException) {
                handleRetry(e, status, statusDesc, retryCount)
            } catch (e: Exception) {
                handleRetry(e, status, statusDesc, retryCount)
            }
            catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                Log.e("API_ERROR", "Code: ${e.code()}, Body: $errorBody", e)
                handleRetry(e, status, statusDesc, retryCount)
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
            postTicketPaymentHistory(status, statusDesc, retryCount + 1)
        } else {
            binding.btnPay.isEnabled = true
            showSnackbar(binding.root, "Failed: ${e.message}")
        }
    }

    private fun handleTicketTransactionStatus(
        status: String,
        orderId: String,
        ticket: String?,
        totalAmount: Double
    ) {

        val intent = Intent(this, PaymentActivity::class.java).apply {
            putExtra("from", "billing")
            putExtra("status", status)
            putExtra("amount", totalAmount.toString())
            putExtra("orderID", orderId)
            putExtra("transID", "")
            putExtra("ticket", ticket)
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
        loadDarshanItems()
    }

    override fun onPause() {
        super.onPause()
        loadDarshanItems()
    }

    override fun onDestroy() {
        super.onDestroy()
        loadDarshanItems()
    }

    override fun onRetryClicked() {
        loadDarshanItems()
    }
    override fun onDialogInactive() {
        loadDarshanItems()
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        loadDarshanItems()
        return super.dispatchTouchEvent(ev)
    }
}